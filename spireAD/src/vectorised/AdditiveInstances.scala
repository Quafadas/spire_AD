package io.github.quafadas.spireAD

import scala.specialized as sp
import vecxt.BoundsCheck.DoBoundsCheck.yes
import cats.kernel.Semigroup
import vecxt.all.*
import cats.Id
import scala.annotation.targetName
import scala.reflect.ClassTag

object VectorisedField:

  given elementwiseMatrixDoubleField: VectorisedField[Matrix, Double] = new VectorisedField[Matrix, Double]:

    // extension (a: Matrix[Double]) override inline def +(x: Matrix[Double]): Matrix[Double] = ???
    // end extension

    // extension (a: Matrix[Double]) override inline def +(x: Double): Matrix[Double] = ???
    // end extension

    // extension (a: Matrix[Double]) override inline def -(x: Double): Matrix[Double] = ???
    // end extension
    def fromDouble(x: Double): Double = x

    def zero(x: Matrix[Double]): Matrix[Double] = Matrix.zeros[Double](x.shape)
    def one(x: Matrix[Double])(using ClassTag[Double]) = Matrix.eye(x.shape(0))

    def allOnes(x: Matrix[Double])(using ClassTag[Double]) = Matrix.ones(x.shape)

    extension (a: Double) def const: Double = a
    end extension

    extension (a: Matrix[Double])
      inline def urnary_- : Matrix[Double] = vecxt.all.*(a)(-1)
      inline def +(x: Matrix[Double]): Matrix[Double] = vecxt.all.+(x)(a)

      @targetName("lhs+")
      inline def +(x: Double): Matrix[Double] = vecxt.all.+(a)(x)

      inline def -(y: Matrix[Double]): Matrix[Double] = vecxt.all.-(a)(y)

      @targetName("lhs-")
      inline def -(x: Double): Matrix[Double] = vecxt.all.-(a)(x)

      inline def *(y: Matrix[Double]): Matrix[Double] = vecxt.all.*(a)(y)
      inline def /(y: Matrix[Double]): Matrix[Double] = vecxt.all./(a)(y)

    end extension

    given elementwiseArrayDoubleField: VectorisedField[Array, Double] = new VectorisedField[Array, Double]:
      def fromDouble(x: Double): Double = x
      def zero(x: Array[Double]): Array[Double] = Array.fill[Double](x.length)(0.0)
      def one(x: Array[Double])(using ClassTag[Double]): Array[Double] = Array.fill[Double](x.length)(1.0)

      def allOnes(x: Array[Double])(using ClassTag[Double]) = one(x)

      extension (a: Double) def const: Double = a

      end extension

      extension (a: Array[Double])
        inline def /(y: Array[Double]): Array[Double] = vecxt.arrays./(a)(y)
        inline def urnary_- : Array[Double] = vecxt.arrays.*(a)(-1)
        inline def +(x: Array[Double]): Array[Double] = vecxt.arrays.+(x)(a)

        @targetName("lhs+")
        inline def +(x: Double): Array[Double] = vecxt.arrays.+(a)(x)

        inline def -(y: Array[Double]): Array[Double] = vecxt.arrays.-(a)(y)
        @targetName("lhs-")
        inline def -(x: Double): Array[Double] = vecxt.arrays.-(a)(x)

        inline def *(y: Array[Double]): Array[Double] = vecxt.arrays.*(a)(y)

      end extension

end VectorisedField

trait VectorisedField[F[_], @sp(Double) A]:
  def fromDouble(x: Double): A
  def zero(x: F[A]): F[A]
  def one(x: F[A])(using ClassTag[A]): F[A]
  def allOnes(x: F[A])(using ClassTag[A]): F[A]

  extension (a: Double) def const: A
  end extension

  extension (a: F[A])
    def urnary_- : F[A]

    def +(x: F[A]): F[A]
    @targetName("lhs+")
    def +(x: A): F[A]
    def -(y: F[A]): F[A]

    def *(y: F[A]): F[A]
    def /(y: F[A]): F[A]
  end extension
end VectorisedField

object VectorisedAdditiveMonoids:

  given additiveArrayMonoidMat: VectorisedAdditiveGroup[Matrix, Double] = new VectorisedAdditiveGroup[Matrix, Double]:
    def empty(hasDim: Matrix[Double]): Matrix[Double] = Matrix.zeros[Double](hasDim.shape)
    def combine(x: Matrix[Double], y: Matrix[Double]): Matrix[Double] = vecxt.all.+(x)(y)
    override def repeatedCombineN(a: Matrix[Double], n: Int): Matrix[Double] = a * n
    // Members declared in io.github.quafadas.spireAD.VectorisedAdditiveGroup
    def inverse(a: Matrix[Double]): Matrix[Double] = negate(a)

    extension (a: Matrix[Double])
      inline def negate: Matrix[Double] = a * -1
      inline def sum: Double = vecxt.arrays.sum(a.raw)
      inline def +(b: Matrix[Double]): Matrix[Double] = vecxt.all.+(a)(b)
      inline def -(b: Matrix[Double]): Matrix[Double] = vecxt.all.-(a)(b)
    end extension

  given additiveArrayMonoid: VectorisedAdditiveGroup[Array, Double] = new VectorisedAdditiveGroup[Array, Double]:
    def empty(hasDim: Array[Double]): Array[Double] = Array.fill[Double](hasDim.length)(0.0)
    def combine(x: Array[Double], y: Array[Double]): Array[Double] = vecxt.arrays.+(x)(y)
    override def repeatedCombineN(a: Array[Double], n: Int): Array[Double] = a * n
    // Members declared in io.github.quafadas.spireAD.VectorisedAdditiveGroup
    def inverse(a: Array[Double]): Array[Double] = negate(a)

    extension (a: Array[Double])
      inline def negate: Array[Double] = a * -1
      inline def sum: Double = vecxt.arrays.sum(a)
      inline def +(b: Array[Double]): Array[Double] = vecxt.arrays.+(a)(b)
      inline def -(b: Array[Double]): Array[Double] = vecxt.arrays.-(a)(b)
    end extension

    // Members declared in io.github.quafadas.spireAD.VectorisedAdditiveSemigroup

  given additiveVectorMonoid: VectorisedMonoid[Vector, Double] = new VectorisedMonoid[Vector, Double]:
    def empty(hasDim: Vector[Double]): Vector[Double] = Vector.fill[Double](hasDim.length)(0.0)
    def combine(x: Vector[Double], y: Vector[Double]): Vector[Double] = x.zip(y).map((a, b) => a + b)
    override def repeatedCombineN(a: Vector[Double], n: Int): Vector[Double] = a.map(_ * n)

  given additiveIntVectorMonoid: VectorisedMonoid[Vector, Int] = new VectorisedMonoid[Vector, Int]:
    def empty(hasDim: Vector[Int]): Vector[Int] = Vector.fill[Int](hasDim.length)(0)
    def combine(x: Vector[Int], y: Vector[Int]): Vector[Int] = x.zip(y).map((a, b) => a + b)
    override def repeatedCombineN(a: Vector[Int], n: Int): Vector[Int] = a.map(_ * n)

end VectorisedAdditiveMonoids

trait VectorisedAdditiveGroup[F[_], @sp(Double) A: Semigroup] extends VectorisedAdditiveMonoid[F, A]:

  /** Find the inverse of `a`.
    *
    * `combine(a, inverse(a))` = `combine(inverse(a), a)` = `empty`.
    *
    * Example:
    * {{{
    * scala> import cats.kernel.instances.int._
    *
    * scala> Group[Int].inverse(5)
    * res0: Int = -5
    * }}}
    */
  def inverse(a: F[A]): F[A]

  /** Remove the element `b` from `a`.
    *
    * Equivalent to `combine(a, inverse(b))`
    *
    * Example:
    * {{{
    * scala> import cats.kernel.instances.int._
    *
    * scala> Group[Int].remove(5, 2)
    * res0: Int = 3
    * }}}
    */
  def remove(a: F[A], b: F[A]): F[A] = combine(a, inverse(b))

  extension (a: F[A])

    inline def -(b: F[A]): F[A]

    inline def negate: F[A]
  end extension

end VectorisedAdditiveGroup

trait VectorisedAdditiveMonoid[F[_], @sp(Double) A: Semigroup]
    extends VectorisedAdditiveSemigroup[F, A]
    with VectorisedMonoid[F, A]:

  inline def zero(a: F[A]) = empty(a)

  extension (a: F[A]) inline def sum: A
  end extension
end VectorisedAdditiveMonoid

trait VectorisedAdditiveSemigroup[F[_], @sp(Int, Long, Float, Double) A] extends Semigroup[F[A]]:
  extension (a: F[A])

    inline def +(b: F[A]): F[A]

  end extension

end VectorisedAdditiveSemigroup
