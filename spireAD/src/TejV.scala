package io.github.quafadas.spireAD

import scala.math.*
import scala.reflect.*

import spire.algebra.*
import spire.syntax.isReal.*
import spire.syntax.vectorSpace.*
import scala.util.chaining.*
import scala.specialized as sp
import java.util.UUID

case class TejVGraph[T: ClassTag]():

  final val dag = DAGV[T, VNode[?, T]]()

  inline def addToGraph[F[_]](t: TejV[F, T])(using
      vf: VectorisedField[F, T],
      vt: VectorisedTrig[F, T]
  ): Unit =
    val n = VConstNode(t.tejNum, t.id)
    dag.addNode(n)
  end addToGraph

  inline def unary[F[_]](op: UrnaryNode[F, T]): Unit =
    dag.addNode(op)
    dag.addEdge(op.depId, op.thisId)
    ()
  end unary

  // inline def binary(
  //     lhs: Tej[T],
  //     rhs: Tej[T],
  //     op: TejOpBinary[T]
  // ): Tej[T] =
  //   val l = TejNode(lhs)
  //   val r = TejNode(rhs)
  //   dag.addNode(op)
  //   dag.addEdge(l, op)
  //   dag.addEdge(r, op)
  //   op.value
  // end binary
end TejVGraph

object TejV extends TejInstances:

  def apply[F[_], T](t: F[T])(using td: TejVGraph[T], f: VectorisedField[F, T], tr: VectorisedTrig[F, T]): TejV[F, T] =
    val tn = new TejV(tejNum = t)
    td.addToGraph(tn)
    tn
  end apply

end TejV

@SerialVersionUID(0L)
final case class TejV[F[_], @sp(Float, Double) T] private (tejNum: F[T]):
  lazy val id = UUID.randomUUID()

  def exp(using t: VectorisedTrig[F, T], f: VectorisedField[F, T], td: TejVGraph[T]) =
    val underlying = this.tejNum.exp
    val expd = new TejV(tejNum = underlying)
    val node = UrnaryNode(UrnaryOps.Exp, underlying, expd.id, this.id)
    td.unary(node)
    expd
  end exp

  def backward(wrt: Seq[TejV[F, T]])(using td: TejVGraph[T]) =
    val graph = td.dag.toposort
    val reversed = graph.reverse
    reversed.foreach { node =>
      node.backward
    }
    val ids = wrt.map(_.id)
    td.dag.getAllNodes.filter(n => ids.contains(n.id)).map((n: VNode[?, T]) => (n.value, n.grad))
  end backward

end TejV

trait TejVInstances[F[_], T]:
  implicit def tejVAlgebra(using
      c: ClassTag[T]
  ): TejVAlgebra[F, T] = new TejVAlgebra[F, T] {}

end TejVInstances

class TejVAlgebra[F[_], @sp(Float, Double) T](using
    c: ClassTag[T],
    t: VectorisedTrig[F, T]
) extends VectorisedTrig[F, T]:
  export t.*
end TejVAlgebra
