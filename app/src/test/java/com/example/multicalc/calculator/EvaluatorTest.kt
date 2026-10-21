package com.example.multicalc.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EvaluatorTest {
    private fun deg(s: String) = Evaluator(isRadians = false).evaluate(s)
    private fun rad(s: String) = Evaluator(isRadians = true).evaluate(s)

    @Test fun basic() {
        assertEquals(14.0, deg("2+3×4"), 1e-12)
        assertEquals(20.0, deg("(2+3)×4"), 1e-12)
        assertEquals(2.5, deg("5÷2"), 1e-12)
        assertEquals(-1.0, deg("2−3"), 1e-12)
    }

    @Test fun powerAndUnary() {
        assertEquals(-4.0, deg("−2^2"), 1e-12)
        assertEquals(512.0, deg("2^3^2"), 1e-12)
        assertEquals(0.5, deg("2^(−1)"), 1e-12)
        assertEquals(25.0, deg("5²"), 1e-12)
        assertEquals(-2.0, deg("(−8)^(1÷3)"), 1e-12)
        assertEquals(2.0, deg("8^(1÷3"), 1e-12)
    }

    @Test fun percent() {
        assertEquals(55.0, deg("50+10%"), 1e-12)
        assertEquals(45.0, deg("50−10%"), 1e-12)
        assertEquals(5.0, deg("50×10%"), 1e-12)
        assertEquals(0.1, deg("10%"), 1e-12)
    }

    @Test fun functions() {
        assertEquals(0.5, deg("sin(30)"), 1e-12)
        assertEquals(1.0, rad("sin(π÷2)"), 1e-12)
        assertEquals(30.0, deg("sin⁻¹(0.5)"), 1e-9)
        assertEquals(3.0, deg("√(9)"), 1e-12)
        assertEquals(3.0, deg("∛(27)"), 1e-12)
        assertEquals(2.0, deg("log(100)"), 1e-12)
        assertEquals(3.0, deg("log₂(8)"), 1e-12)
        assertEquals(1.0, deg("ln(e)"), 1e-12)
        assertEquals(120.0, deg("5!"), 1e-12)
        assertEquals(1000.0, deg("10^(3"), 1e-12)
        assertEquals(0.0, deg("cos(90)"), 1e-12)
    }

    @Test fun implicitMultiplicationAndEe() {
        assertEquals(2 * Math.PI, deg("2π"), 1e-12)
        assertEquals(12.0, deg("3(4)"), 1e-12)
        assertEquals(1.0, deg("2sin(30)"), 1e-12)
        assertEquals(1500.0, deg("1.5E3"), 1e-12)
        assertEquals(0.015, deg("1.5E−2"), 1e-12)
        assertEquals(Math.E * 0.5, deg("esin(30)"), 1e-12)
    }

    @Test fun errors() {
        assertThrows(CalcException::class.java) { deg("1÷0") }
        assertThrows(CalcException::class.java) { deg("tan(90)") }
        assertThrows(CalcException::class.java) { deg("√(−1)") }
        assertThrows(CalcException::class.java) { deg("2+") }
    }

    @Test fun formatting() {
        assertEquals("1,234,567.89", Evaluator.format(1234567.89))
        assertEquals("0.3", Evaluator.format(0.1 + 0.2))
        assertEquals("1.23456789e20", Evaluator.format(1.23456789e20))
        assertEquals("-5", Evaluator.format(-5.0))
    }
}
