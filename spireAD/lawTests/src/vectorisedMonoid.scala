package io.github.quafadas.spireAD

/*
 * Copyright (c) 2015 Typelevel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import scala.{specialized => sp}
import cats.kernel.Eq
import vecxt.arrays.+
import vecxt.arrays.*
import vecxt.BoundsCheck.DoBoundsCheck.yes



object VectorisedMonoid {

  given additiveArrayMonoid : VectorisedMonoid[Array, Double] = new VectorisedMonoid[Array, Double] {
    def empty(hasDim: Array[Double]): Array[Double] = Array.fill[Double](hasDim.length)(0.0)
    def combine(x: Array[Double], y: Array[Double]): Array[Double] = x + y
    override def repeatedCombineN(a: Array[Double], n: Int): Array[Double] = a *  n
  }

  given additiveVectorMonoid : VectorisedMonoid[Vector, Double] = new VectorisedMonoid[Vector, Double] {
    def empty(hasDim: Vector[Double]): Vector[Double] = Vector.fill[Double](hasDim.length)(0.0)
    def combine(x: Vector[Double], y: Vector[Double]): Vector[Double] = x.zip(y).map((a, b) => a + b) 
    override def repeatedCombineN(a: Vector[Double], n: Int): Vector[Double] = a.map(_ *  n)
  }

}

/**
 * A monoid is a semigroup with an identity. A monoid is a specialization of a
 * semigroup, so its operation must be associative. Additionally,
 * `combine(x, empty) == combine(empty, x) == x`. For example, if we have `Monoid[String]`,
 * with `combine` as string concatenation, then `empty = ""`.
 */
trait VectorisedMonoid[F[_], @sp(Double) A] extends Any with cats.kernel.Semigroup[F[A]] { self =>

  /**
   * Return the identity element for this monoid.
   *
   * Example:
   * {{{
   * scala> import cats.kernel.instances.int._
   * scala> import cats.kernel.instances.string._
   *
   * scala> Monoid[String].empty
   * res0: String = ""
   *
   * scala> Monoid[Int].empty
   * res1: Int = 0
   * }}}
   */
  def empty(hasDim: F[A]): F[A]

  /**
   * Tests if `a` is the identity.
   *
   * Example:
   * {{{
   * scala> import cats.kernel.instances.string._
   *
   * scala> Monoid[String].isEmpty("")
   * res0: Boolean = true
   *
   * scala> Monoid[String].isEmpty("something")
   * res1: Boolean = false
   * }}}
   */
  def isEmpty(a: F[A])(implicit ev: Eq[F[A]]): Boolean =
    ev.eqv(a, empty(a))

  /**
   * Return `a` appended to itself `n` times.
   *
   * Example:
   * {{{
   * scala> import cats.kernel.instances.string._
   *
   * scala> Monoid[String].combineN("ha", 3)
   * res0: String = hahaha
   *
   * scala> Monoid[String].combineN("ha", 0)
   * res1: String = ""
   * }}}
   */
  override def combineN(a: F[A], n: Int): F[A] =
    if (n < 0) throw new IllegalArgumentException("Repeated combining for monoids must have n >= 0")
    else if (n == 0) empty(a)
    else repeatedCombineN(a, n)

  /**
   * Given a sequence of `as`, sum them using the monoid and return the total.
   *
   * Example:
   * {{{
   * scala> import cats.kernel.instances.string._
   *
   * scala> Monoid[String].combineAll(List("One ", "Two ", "Three"))
   * res0: String = One Two Three
   *
   * scala> Monoid[String].combineAll(List.empty)
   * res1: String = ""
   * }}}
   */
  def combineAll(as: IterableOnce[F[A]]): F[A] =
    val head = as.iterator
    head.hasNext match
      case false => throw new IllegalArgumentException("Cannot combine empty iterator.")
      case true =>
        var acc = head.next()
        while (head.hasNext) acc = combine(acc, head.next())
        acc    

  override def combineAllOption(as: IterableOnce[F[A]]): Option[F[A]] =
    if (as.iterator.isEmpty) None else Some(combineAll(as))

  // override def reverse: Monoid[A] =
  //   new Monoid[A] {
  //     def empty = self.empty
  //     def combine(a: A, b: A) = self.combine(b, a)
  //     // a + a + a + ... is the same when reversed
  //     override def combineN(a: A, n: Int): A = self.combineN(a, n)
  //     override def reverse = self
  //   }
}

