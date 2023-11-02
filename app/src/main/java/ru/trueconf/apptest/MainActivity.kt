package ru.trueconf.apptest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Build



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
import kotlin.coroutines.resume

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
        setupHelloTextView()
        setupTouchListeners()
    }
        private fun setupHelloTextView() {
            val layoutParams = binding.hello.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.apply {
                startToStart = ConstraintLayout.LayoutParams.UNSET
                endToEnd = ConstraintLayout.LayoutParams.UNSET
                topToTop = ConstraintLayout.LayoutParams.UNSET
                bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            }
            binding.hello.layoutParams = layoutParams
        }
    private fun setupTouchListeners() {
        binding.hello.setOnTouchListener { _, event -> handleHelloTouch(event) }
        binding.parentLayout.setOnTouchListener { _, event -> handleParentLayoutTouch(event) }
    }
    private fun handleHelloTouch(event: MotionEvent): Boolean {
        return if (event.action == MotionEvent.ACTION_DOWN) {
            cancelAnimationsAndCoroutines()
            true
        } else false
    }
    private fun handleParentLayoutTouch(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            cancelAnimationsAndCoroutines()
            moveHelloTo(event.rawX, event.rawY)
            setColorBasedOnLocale()
            lifeCycleCoroutine = coroutineScope.launch {
                delay(5000)
                moveText()
            }
        }
        return true
    }
    private fun cancelAnimationsAndCoroutines() {
        animator?.cancel()
        lifeCycleCoroutine?.cancel()
    }

    private fun moveHelloTo(rawX: Float, rawY: Float) {
        val newX = (rawX - binding.hello.width / 2).coerceIn(0f, binding.root.width - binding.hello.width.toFloat())
        val newY = (rawY - binding.hello.height / 2).coerceIn(0f, binding.root.height - binding.hello.height.toFloat())
        binding.hello.x = newX
        binding.hello.y = newY
    }
    private fun setColorBasedOnLocale() {
        val currentLocale = getCurrentLocale()
        when (currentLocale) {
            "en" -> binding.hello.setTextColor(Color.RED)
            "ru" -> binding.hello.setTextColor(Color.BLUE)

        }
    }

    private fun getCurrentLocale(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales[0].language
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale.language
        }
    }

    private suspend fun moveText() {
        while (true) {
            val endY = if (binding.hello.y >= binding.root.height - binding.hello.height) {
                isMovingDown = false
                0f
            } else {
                isMovingDown = true
                binding.root.height - binding.hello.height.toFloat()
            }
            animator = ObjectAnimator.ofFloat(binding.hello, "y", binding.hello.y, endY).apply {
                duration = 10000
                start()
                suspendAndAwaitEnd()
            }
            isMovingDown = !isMovingDown
        }
    }
    private suspend fun ObjectAnimator.suspendAndAwaitEnd() =
        suspendCancellableCoroutine { continuation ->
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    removeListener(this)
                    continuation.resume(Unit)
                }
            })
            continuation.invokeOnCancellation { cancel() }
        }

    override fun onDestroy() {
        super.onDestroy()
        cancelAnimationsAndCoroutines()
        coroutineScope.cancel()
    }
}
