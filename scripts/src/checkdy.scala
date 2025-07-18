import io.github.quafadas.inspireRAD.*
import vecxt.all.*
import scala.reflect.ClassTag
import vecxt.BoundsCheck.DoBoundsCheck.yes
import cats.syntax.all.toShow
import cats.Show

import spire.*
import spire.math.JetDim
import spire.implicits.*
import _root_.algebra.ring.Field
import spire.compat.numeric
import spire.math.Jet
import vecxt.all.shape

given Show[Matrix[Double]] with
  def show(matrix: Matrix[Double]): String =
    val rows =
      for i <- 0 until matrix.rows
      yield matrix
        .row(i)
        .map(s => "%.3f".format(s).reverse.padTo(6, ' ').reverse)
        .mkString(" | ")
    val footer = ("-" * (rows.head.length))
    (rows :+ footer).mkString("\n")
  end show
end given

given jd2: Show[Matrix[Jet[Double]]] = new Show[Matrix[Jet[Double]]]:
  def show(matrix: Matrix[Jet[Double]]): String =
    val rows =
      for i <- 0 until matrix.rows
      yield matrix
        .row(i)
        .map(s => s.toString().reverse.padTo(10, ' ').reverse)
        .mkString(" | ")
    val footer = ("-" * (rows.head.length))
    (rows :+ footer).mkString("\n")
  end show

given jd3: Show[Array[Jet[Double]]] = new Show[Array[Jet[Double]]]:
  def show(arr: Array[Jet[Double]]): String =
    arr
      .map(s => s.toString().reverse.padTo(10, ' ').reverse)
      .mkString("[", "\n", "]")

  end show

given Show[Array[Double]] with
  def show(arr: Array[Double]): String =
    arr.mkString("[", ", ", "]")
end given

given Show[Scalar[Double]] with
  def show(arr: Scalar[Double]): String =
    arr.scalar.toString
end given

@main def checkdy =
  inline def calcLoss[@specialized(Double) T](
      weights: Matrix[T],
      incomingData: Matrix[T],
      targets: Array[Int]
  )(using
      inline mOps: Matrixy[Matrix, T],
      inline fas: VectorisedField[Scalar, T],
      inline faa: VectorisedField[Array, T],
      inline fm: VectorisedField[Matrix, T],
      inline fa: VectorisedTrig[Array, T],
      inline t: VectorisedTrig[Matrix, T],
      inline redArr: Reductions[Array, T, 1],
      inline redMat: Reductions[Matrix, T, 2],
      inline showable: Show[Matrix[T]],
      nt: Numeric[T],
      ct: ClassTag[T]
  ): Scalar[T] =
    val logits = incomingData @@ weights
    // println(logits.show)
    val counts = logits.exp
    val probsNN = counts.mapRows(row => row / row.sum)
    println("normaliseRows(probsNN).show")
    println(probsNN.show)
    val range = (0 until targets.length).toArray.zip(targets)
    val ranged = mOps.apply(probsNN)(range)
    -Scalar(ranged.mapRowsToScalar(_.sum).log.mean)
  end calcLoss

  val mat1 = Matrix.fromRows(
    Array(1.0, 2.0, 3.0, 4.0),
    Array(1.0, 2.0, 3.0, 4.0),
    Array(1.0, 2.0, 3.0, 4.0),
    Array(1.0, 2.0, 3.0, 4.0)
  )

  val mat2 = Matrix.fromRows(
    Array(1.0, 2.0, 3.0, 4.0) / 10,
    Array(1.0, 2.0, 3.0, 4.0) / 10
  )

  val targets = Array(1, 0)

  val calc1 = calcLoss(
    mat1,
    mat2,
    targets
  )

  println("calc1:")
  println(calc1.show)
  println("-----")

  {
    given jd: JetDim = JetDim(16)
    import VectorisedField.jetNumeric

    given vf: VectorisedField[Array, Jet[Double]] = VectorisedField.elementwiseArrayJetField
    given vm: VectorisedField[Matrix, Jet[Double]] = VectorisedField.elementwiseMatrixJetField
    given vs: VectorisedField[Scalar, Jet[Double]] = VectorisedField.scalarJetField
    given rja: Reductions[Array, Jet[Double], 1] = Reductions.vtaJet
    given rjm: Reductions[Matrix, Jet[Double], 2] = Reductions.vtmJet

    given doubleJM: Matrixy[Matrix, Jet[Double]] = Matrixy.doubleJetIsMatrixy(using yes, jd)

    val calcJet = calcLoss(
      Matrix.fromRows(
        Array(1.0, 2.0, 3.0, 4.0).jetArr,
        Array(1.0, 2.0, 3.0, 4.0).jetArr(4),
        Array(1.0, 2.0, 3.0, 4.0).jetArr(8),
        Array(1.0, 2.0, 3.0, 4.0).jetArr(12)
      ),
      Matrix.fromRows(
        (Array(1.0, 2.0, 3.0, 4.0) / 10).jetArrNoGrad,
        (Array(1.0, 2.0, 3.0, 4.0) / 10).jetArrNoGrad
      ),
      Array(1, 0)
    )

    println("jet Calc:")
    println(calcJet)
    println("-----")
  }

  given jd: JetDim = JetDim(16)

  println("-----")
  val part1 = Matrix.fromRows(
    (Array(1.0, 2.0, 3.0)).jetArr(0),
    (Array(4.0, 5.0, 6.0)).jetArr(3),
    (Array(4.0, 5.0, 6.0)).jetArr(6)
  )

  // val newMat = part1.mapRows { row =>
  //   val sum = row.foldLeft(Jet(0.0))(_ + _)
  //   row.map(_ / sum)
  // }

  given tvg: TejVGraph[Double] = TejVGraph[Double]()

  println("instantiate")

  val tv1 = TejV(mat1)
  val tv2 = TejV(mat2)

  val logits = tv2 @@ tv1

  val counts = logits.exp
  println("counts")
  println("counts.shape: " + counts.value.shape)

  val probsNN = counts.normaliseRowsL1

  println("probsNN")

  val range = (0 until targets.length).toArray.zip(targets)

  println("range")
  println(range.mkString("[", ", ", "]"))
  val ranged = probsNN(range)
  println("ranged")
  println(ranged.value.show)
  val loss = ranged.mapRowsToScalar(ReductionOps.Sum)
  println("mapped to scalars")
  val scalarLoss = loss.log.mean

  // val grad =  scalarLoss.backward[Matrix](Set(tv1))
  // println("scalarLoss")
  // println(scalarLoss.value)
  // println("grad")
  // println(grad.head.grad.printMat)

  val grad2 = scalarLoss.backward((tv1 = tv1))

  println("grad")
  println(grad2.tv1.printMat)

end checkdy
