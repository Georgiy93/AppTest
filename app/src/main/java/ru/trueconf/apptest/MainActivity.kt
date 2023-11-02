package ru.trueconf.apptest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Build


import android.util.Log
import android.view.MotionEvent

import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.trueconf.apptest.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var isMovingDown = true
    private var animator: ObjectAnimator? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var lifeCycleCoroutine: Job? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val layoutParams = binding.hello.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.startToStart = ConstraintLayout.LayoutParams.UNSET
        layoutParams.endToEnd = ConstraintLayout.LayoutParams.UNSET
        layoutParams.topToTop = ConstraintLayout.LayoutParams.UNSET
        layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
        binding.hello.layoutParams = layoutParams
        binding.hello.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {

                animator?.cancel()
                lifeCycleCoroutine?.cancel()
                true
            } else {
                false
            }
        }
        binding.parentLayout.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {

                    animator?.cancel()
                    lifeCycleCoroutine?.cancel()
                    isMovingDown = true

                    var newX = event.rawX - binding.hello.width / 2
                    var newY = event.rawY - binding.hello.height / 2

                    if (newX < 0) {
                        newX = 0f
                    } else if (newX + binding.hello.width > binding.root.width) {
                        newX = binding.root.width - binding.hello.width.toFloat()
                    }


                    if (newY < 0) {
                        newY = 0f
                    } else if (newY + binding.hello.height > binding.root.height) {
                        newY = binding.root.height - binding.hello.height.toFloat()
                    }
                    binding.hello.x = newX
                    binding.hello.y = newY


                    lifeCycleCoroutine = coroutineScope.launch {
                        delay(5000)
                        moveText()
                    }


                    val currentLocale: String =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            resources.configuration.locales[0].language
                        } else {
                            @Suppress("DEPRECATION")
                            resources.configuration.locale.language
                        }

                    when (currentLocale) {
                        "en" -> binding.hello.setTextColor(Color.RED)
                        "ru" -> binding.hello.setTextColor(Color.BLUE)
                    }

                    view.performClick()
                }
            }
            true
        }
    }

    private suspend fun moveText() {
        while (true) {
            if (binding.hello.y >= binding.root.height - binding.hello.height) {
                isMovingDown = false
            }

            if (binding.hello.y <= 0) {
                isMovingDown = true
            }

            val endY = if (isMovingDown) {
                binding.root.height - binding.hello.height.toFloat()
            } else {
                0f
            }
            Log.d(
                "AnimationDebug",
                "Starting animation towards: ${if (isMovingDown) "Bottom" else "Top"}"
            )
            animator = ObjectAnimator.ofFloat(binding.hello, "y", binding.hello.y, endY)
            animator?.duration = 10000
            animator?.start()
            animator?.suspendAndAwaitEnd()
            isMovingDown = !isMovingDown
        }
    }

    private suspend fun ObjectAnimator.suspendAndAwaitEnd() =
        suspendCancellableCoroutine { continuation ->
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    removeListener(this)
                    continuation.resume(Unit) {}
                }
            })
            continuation.invokeOnCancellation { cancel() }
        }

    override fun onDestroy() {
        super.onDestroy()
        lifeCycleCoroutine?.cancel()
        coroutineScope.cancel()
    }
}
