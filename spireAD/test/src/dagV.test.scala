package io.github.quafadas.spireAD

import munit.*

import spire.*
import spire.math.*
import spire.implicits.DoubleAlgebra
import cats.Show
import spire.implicits.ArrayNormedVectorSpace

class DAGVSuite extends FunSuite:

  import VectorisedTrig.vta
  import VectorisedField.elementwiseArrayDoubleField

  given Show[Array[Double]] with
    def show(arr: Array[Double]): String = arr.mkString("[", ", ", "]")
  end given

  test("f(x) = exp(sin(x))") {

    val arr = Array(1.0, 2.0, 3.0)
    given jd: JetDim = JetDim(3)
    val jetArr = arr.jetArr.map(sin).map(exp)

    // println(jetArr.mkString("\n"))

    given tejV: TejVGraph[Double] = TejVGraph[Double]()

    val tej = TejV(arr)

    val fct = tej.sin.exp

    for i <- 0 until arr.length do
      assertEquals(
        fct.value(i),
        jetArr(i).real
      )
    end for

    val out = fct.backward(Set(tej))

    assert(out.size == 1)
    // println(out.head.grad.mkString("\n"))

    for i <- 0 until arr.length do
      assertEquals(
        out.head.grad(i),
        jetArr(i).infinitesimal(i)
      )
    end for

  }

  test("TejV +") {

    val (arr1, arr2, jetArrPlus) = prepareJetArrays(_ + _)
    // println(jetArrPlus.mkString("\n"))

    given tejV: TejVGraph[Double] = TejVGraph[Double]()

    val tej1 = TejV(arr1)
    val tej2 = TejV(arr2)

    val tejPlus = tej1 + tej2

    for i <- 0 until arr1.length do
      assertEquals(
        tejPlus.value(i),
        jetArrPlus(i).real
      )
    end for

    val out = tejPlus.backward(Set(tej1, tej2))

    assert(out.size == 2)
    val grad = out.head.grad + out.last.grad

    for i <- 0 until arr1.length do
      assertEquals(
        grad(i),
        jetArrPlus(i).infinitesimal(i)
      )
    end for
  }
  test("TejV -") {

    val (arr1, arr2, jetArrPlus) = prepareJetArrays(_ - _)

    given tejV: TejVGraph[Double] = TejVGraph[Double]()

    val tej1 = TejV(arr1)
    val tej2 = TejV(arr2)

    val tejPlus = tej1 - tej2

    for i <- 0 until arr1.length do
      assertEquals(
        tejPlus.value(i),
        jetArrPlus(i).real
      )
    end for

    val out = tejPlus.backward(Set(tej1, tej2))

    assert(out.size == 2)
    val grad = out.head.grad + out.last.grad

    for i <- 0 until arr1.length do
      assertEquals(
        grad(i),
        jetArrPlus(i).infinitesimal(i)
      )
    end for
  }
  test("TejV *") {

    val (arr1, arr2, jetArrPlus) = prepareJetArrays(_ * _)

    // println(jetArrPlus.mkString("\n"))

    given tejV: TejVGraph[Double] = TejVGraph[Double]()

    val tej1 = TejV(arr1)
    val tej2 = TejV(arr2)

    val tejPlus = tej1 * tej2

    for i <- 0 until arr1.length do
      assertEquals(
        tejPlus.value(i),
        jetArrPlus(i).real
      )
    end for

    val out = tejPlus.backward(Set(tej1, tej2))

    assert(out.size == 2)
    val grad = out.head.grad + out.last.grad

    for i <- 0 until arr1.length do
      assertEquals(
        grad(i),
        jetArrPlus(i).infinitesimal(i)
      )
    end for
  }

  def prepareJetArrays(
      op: (Jet[Double], Jet[Double]) => Jet[Double]
  ): (Array[Double], Array[Double], Array[Jet[Double]]) =
    given jd: JetDim = JetDim(3)
    val arr1 = Array(1.0, 2.0, 3.0)
    val arr2 = Array(4.0, 5.0, 8.0)

    val jetArr1 = arr1.jetArr
    val jetArr2 = arr2.jetArr

    val jetArrPlus = jetArr1.zip(jetArr2).map((a, b) => op(a, b))
    (arr1, arr2, jetArrPlus)
  end prepareJetArrays

  test("TejV /") {

    val (arr1, arr2, jetArrPlus) = prepareJetArrays(_ / _)

    // println(jetArrPlus.mkString("\n"))

    given tejV: TejVGraph[Double] = TejVGraph[Double]()
    val tej1 = TejV(arr1)
    val tej2 = TejV(arr2)

    val tejPlus = tej1 / tej2

    for i <- 0 until arr1.length do
      assertEqualsDouble(
        tejPlus.value(i),
        jetArrPlus(i).real,
        0.0000001
      )
    end for

    val out = tejPlus.backward(Set(tej1, tej2))

    assert(out.size == 2)
    val grad = out.head.grad + out.last.grad

    for i <- 0 until arr1.length do
      assertEqualsDouble(
        grad(i),
        jetArrPlus(i).infinitesimal(i),
        0.0000001
      )
    end for
  }

  test("Adding the same container without the Tej wqrapper") {
    given tejV: TejVGraph[Double] = TejVGraph[Double]()
    val arr = Array(1.0, 2.0, 3.0, 4.0)
    val tej = TejV(arr)

    val t2 = tej + Array(1.0, 2.0, 3.0, 4.0)
    // TODO ???
    // val t3 = arr + tej
    assert(tejV.dag.toposort.size == 3)
    assertEquals(t2.value.toSeq, Seq(2.0, 4.0, 6.0, 8.0))
  }

end DAGVSuite
