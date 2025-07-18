package io.github.quafadas.inspireRAD

import munit.*

import spire.*
import spire.math.*
import spire.implicits.DoubleAlgebra
import spire.std.array.ArrayVectorSpace

import _root_.algebra.ring.Field

class DAGSuite extends FunSuite:

  test("Add and retrieve nodes") {
    val dag = new DAG[Double]()
    dag.addStringNode("A")
    dag.addStringNode("B")

    assertEquals(
      dag.getAllNodes.asInstanceOf[Set[DebugNode[Double]]],
      Set(DebugNode("A"), DebugNode("B"))
    )
  }

  test("Add edges and check existence") {
    val dag = new DAG[Double]()
    val dna = DebugNode("A")
    val dnb = DebugNode("B")
    dag.addNode(dna)
    dag.addNode(dnb)
    dag.addEdge(dna, dnb)

    assert(dag.hasEdge(dna, dnb))
    assert(!dag.hasEdge(dnb, dna))
  }

  test("Remove nodes and check graph persistence") {
    val dag = new DAG[Double]()
    val dna = DebugNode("A")
    val dnb = DebugNode("B")
    dag.addNode(dna)
    dag.addNode(dnb)
    dag.addEdge(dna, dnb)
    dag.removeNode(dna)

    assertEquals(dag.getAllNodes, Set[AdNode[Double]](dnb))
    assert(!dag.hasEdge(dna, dnb))
  }

  test("Remove edges and confirm") {
    val dag = new DAG[Double]()
    val dna = DebugNode("A")
    val dnb = DebugNode("B")
    dag.addNode(dna)
    dag.addNode(dnb)
    dag.addEdge(dna, dnb)
    dag.removeEdge(dna, dnb)

    assert(!dag.hasEdge(dna, dnb))
  }

  test("Topological sort with acyclic graph") {
    val dag = new DAG[Double]()
    val dna = DebugNode("A")
    val dnb = DebugNode("B")
    val dnc = DebugNode("C")
    dag.addNode(dnc)
    dag.addNode(dnb)
    dag.addNode(dna)
    dag.addEdge(dna, dnb)
    dag.addEdge(dnb, dnc)

    val sorted = dag.toposort
    assertEquals(sorted, List(dna, dnb, dnc))
  }

  test("Topological sort with cyclic graph throws exception") {
    val dag = new DAG[Double]()
    val dna = DebugNode("A")
    val dnb = DebugNode("B")
    dag.addNode(dna)
    dag.addNode(dnb)
    dag.addEdge(dna, dnb)
    dag.addEdge(dnb, dna)

    intercept[IllegalArgumentException] {
      dag.toposort
    }
  }

  test("Graphviz representation correctness") {
    val dag = new DAG[Double]()
    val dna = DebugNode("A")
    val dnb = DebugNode("B")
    dag.addNode(dna)
    dag.addNode(dnb)
    dag.addEdge(dna, dnb)

    // println(dag.toGraphviz.trim)

    val expectedGraphviz =
      """digraph {
        |  "A" -> "B";
        |  "B";
        |}"""

      // TODO renable
    // assertNoDiff(dag.toGraphviz.trim, expectedGraphviz.trim)
  }

  test("Graph is empty") {
    val dag = new DAG[Double]()
    val dna = DebugNode("A")
    assert(dag.isEmpty)

    dag.addNode(dna)
    assert(!dag.isEmpty)

    dag.removeNode(dna)
    assert(dag.isEmpty)
  }

  /** For a single node, check that;
    *   - The name of the node appears in the graph
    *   - That the nodes and edges are in the graph
    *   - That the reverse mode algorithm matches spires forward mode
    */
  def unaryTest(
      fct: Tej[Double] => Unit,
      fctJet: Jet[Double] => Jet[Double],
      opLabel: String
      // jetCheck: Jet[Double]
  )(using td: TejDim[Double], jd: JetDim, f: Field[Double], ct: ClassTag[Double]) =

    val twoD = summon[Field[Double]].one * 2.0
    val two = Tej(twoD)
    val twoj = Jet(twoD) + Jet.h(0)
    // val one = Tej(summon[Field[Double]].one) // corrected back to one
    fct(two)
    assert(td.dag.toGraphviz.contains(opLabel))
    val sorted = td.dag.toposort.reverse
    assert(sorted.size == 2)

    sorted.head.grad = summon[Field[Double]].one

    for node <- sorted.reverse do td.dag.getNode(node.id).backward
    end for

    val forwardVersion = fctJet(twoj)
    assertEqualsDouble(
      sorted.last.grad,
      fctJet(twoj).infinitesimal(0),
      0.0000001
    )

  end unaryTest

  /** For a single binary operation, check that;
    *   - The name of the node appears in the graph
    *   - That the nodes and edges are in the graph
    *   - That the reverse mode algorithm matches spires forward mode
    */
  def binaryTest(
      fct: (Tej[Double], Tej[Double]) => Unit,
      fctJet: (Jet[Double], Jet[Double]) => Jet[Double],
      opLabel: String
  )(using td: TejDim[Double], f: Field[Double], ct: ClassTag[Double], jd: JetDim) =
    val twoD = summon[Field[Double]].one * 2.0
    val threeD = summon[Field[Double]].one * 3.0
    val two = Tej(twoD)
    val three = Tej(threeD)

    val twoj = Jet(twoD) + Jet.h(0)
    val threej = Jet(threeD) + Jet.h(1)

    fct(two, three)
    // println(td.dag.toGraphviz)
    assert(td.dag.toGraphviz.contains(opLabel))
    assertEquals(td.dag.toposort.size, 3)
    val sorted = td.dag.toposort.reverse

    sorted.head.grad = summon[Field[Double]].one

    for node <- sorted.reverse do td.dag.getNode(node.id).backward
    end for

    val forwardVersion = fctJet(twoj, threej)

    assertEqualsDouble(
      sorted.tail.head.grad,
      forwardVersion.infinitesimal(0),
      0.0000001
    )

    assertEqualsDouble(
      sorted.last.grad,
      forwardVersion.infinitesimal(1),
      0.0000001
    )

  end binaryTest

  test("unary nodes : exp") {
    given td: TejDim[Double] = TejDim()
    given jd: JetDim = JetDim(1)
    unaryTest(
      exp[Tej[Double]],
      exp[Jet[Double]],
      "Exp"
    )
  }

  test("unary nodes : sin") {
    given td: TejDim[Double] = TejDim()
    given jd: JetDim = JetDim(1)
    unaryTest(sin[Tej[Double]], sin[Jet[Double]], "Sin")
  }

  test("unary nodes : log") {
    given td: TejDim[Double] = TejDim()
    given jd: JetDim = JetDim(1)
    unaryTest(log[Tej[Double]], log[Jet[Double]], "Log")
  }

  test("unary nodes : cos") {
    given td: TejDim[Double] = TejDim()
    given jd: JetDim = JetDim(1)
    unaryTest(cos[Tej[Double]], cos[Jet[Double]], "Cos")
  }

  test("binary nodes : +") {
    given td: TejDim[Double] = TejDim()
    given jd: JetDim = JetDim(2)
    binaryTest(
      (x: Tej[Double], y: Tej[Double]) => x + y,
      (x: Jet[Double], y: Jet[Double]) => x + y,
      "Add"
    )
  }

  test("binary nodes : -") {
    given td: TejDim[Double] = TejDim()
    given jd: JetDim = JetDim(2)
    binaryTest((x: Tej[Double], y: Tej[Double]) => x - y, (x: Jet[Double], y: Jet[Double]) => x - y, "Sub")
  }

  test("binary nodes : *") {
    given td: TejDim[Double] = TejDim()
    given jd: JetDim = JetDim(2)
    binaryTest((x: Tej[Double], y: Tej[Double]) => x * y, (x: Jet[Double], y: Jet[Double]) => x * y, "Mul")
  }

  test("binary nodes : /") {
    given jd: JetDim = JetDim(2)
    given td: TejDim[Double] = TejDim()
    binaryTest((x: Tej[Double], y: Tej[Double]) => x / y, (x: Jet[Double], y: Jet[Double]) => x / y, "Div")
  }

  test("reachableNodesFromSink - single path") {
    val dag = new DAG[Double]()
    val nodeA: AdNode[Double] = DebugNode("A") // source
    val nodeB: AdNode[Double] = DebugNode("B") // intermediate
    val nodeC: AdNode[Double] = DebugNode("C") // sink

    dag.addNode(nodeA)
    dag.addNode(nodeB)
    dag.addNode(nodeC)
    dag.addEdge(nodeA, nodeB)
    dag.addEdge(nodeB, nodeC)

    val incomingEdges: Map[AdNode[Double], List[AdNode[Double]]] = Map(
      nodeB -> List(nodeA),
      nodeC -> List(nodeB)
    )

    val reachable = dag.reachableNodesFromSink(nodeC, Set(nodeA), incomingEdges)
    assertEquals(reachable, Set(nodeA, nodeB, nodeC))
  }

  test("reachableNodesFromSink - multiple paths") {
    val dag = new DAG[Double]()
    val nodeA: AdNode[Double] = DebugNode("A") // source 1
    val nodeB: AdNode[Double] = DebugNode("B") // source 2
    val nodeC: AdNode[Double] = DebugNode("C") // intermediate
    val nodeD: AdNode[Double] = DebugNode("D") // intermediate
    val nodeE: AdNode[Double] = DebugNode("E") // sink

    dag.addNode(nodeA)
    dag.addNode(nodeB)
    dag.addNode(nodeC)
    dag.addNode(nodeD)
    dag.addNode(nodeE)

    // Create diamond pattern: A->C->E, B->D->E, C->D
    dag.addEdge(nodeA, nodeC)
    dag.addEdge(nodeB, nodeD)
    dag.addEdge(nodeC, nodeE)
    dag.addEdge(nodeD, nodeE)
    dag.addEdge(nodeC, nodeD) // Additional edge creating diamond

    val incomingEdges: Map[AdNode[Double], List[AdNode[Double]]] = Map(
      nodeC -> List(nodeA),
      nodeD -> List(nodeB, nodeC),
      nodeE -> List(nodeC, nodeD)
    )

    val reachable = dag.reachableNodesFromSink(nodeE, Set(nodeA, nodeB), incomingEdges)
    assertEquals(reachable, Set(nodeA, nodeB, nodeC, nodeD, nodeE))
  }

  test("reachableNodesFromSink - partial reachability") {
    val dag = new DAG[Double]()
    val nodeA: AdNode[Double] = DebugNode("A") // source 1 (reachable)
    val nodeB: AdNode[Double] = DebugNode("B") // source 2 (not reachable from sink)
    val nodeC: AdNode[Double] = DebugNode("C") // intermediate from A
    val nodeD: AdNode[Double] = DebugNode("D") // intermediate from B
    val nodeE: AdNode[Double] = DebugNode("E") // sink (only reachable from A path)

    dag.addNode(nodeA)
    dag.addNode(nodeB)
    dag.addNode(nodeC)
    dag.addNode(nodeD)
    dag.addNode(nodeE)

    // A->C->E and B->D (D not connected to E)
    dag.addEdge(nodeA, nodeC)
    dag.addEdge(nodeB, nodeD)
    dag.addEdge(nodeC, nodeE)

    val incomingEdges: Map[AdNode[Double], List[AdNode[Double]]] = Map(
      nodeC -> List(nodeA),
      nodeD -> List(nodeB),
      nodeE -> List(nodeC)
    )

    val reachable = dag.reachableNodesFromSink(nodeE, Set(nodeA, nodeB), incomingEdges)
    // Should only include nodes reachable from sink E
    assertEquals(reachable, Set(nodeA, nodeC, nodeE))
  }

  test("reachableNodesFromSink - sink is source") {
    val dag = new DAG[Double]()
    val nodeA: AdNode[Double] = DebugNode("A") // both source and sink

    dag.addNode(nodeA)

    val incomingEdges = Map.empty[AdNode[Double], List[AdNode[Double]]]

    val reachable = dag.reachableNodesFromSink(nodeA, Set(nodeA), incomingEdges)
    assertEquals(reachable, Set(nodeA))
  }

  test("reachableNodesFromSink - assertions fail for invalid inputs") {
    val dag = new DAG[Double]()
    val nodeA: AdNode[Double] = DebugNode("A")
    val nodeB: AdNode[Double] = DebugNode("B")
    dag.addNode(nodeA)
    // nodeB not added to DAG

    val incomingEdges: Map[AdNode[Double], List[AdNode[Double]]] = Map(nodeA -> List.empty)

    // Sink not in graph
    intercept[AssertionError] {
      dag.reachableNodesFromSink(nodeB, Set(nodeA), incomingEdges)
    }

    // Empty sources
    intercept[AssertionError] {
      dag.reachableNodesFromSink(nodeA, Set.empty, incomingEdges)
    }

    // Source not in graph
    intercept[AssertionError] {
      dag.reachableNodesFromSink(nodeA, Set(nodeB), incomingEdges)
    }
  }

  // Helper method test
  test("computeIncomingEdges helper") {
    val dag = new DAG[Double]()
    val nodeA: AdNode[Double] = DebugNode("A")
    val nodeB: AdNode[Double] = DebugNode("B")
    val nodeC: AdNode[Double] = DebugNode("C")

    dag.addNode(nodeA)
    dag.addNode(nodeB)
    dag.addNode(nodeC)
    dag.addEdge(nodeA, nodeB)
    dag.addEdge(nodeB, nodeC)
    dag.addEdge(nodeA, nodeC) // Direct edge A->C

    val incomingEdges = dag.computeIncomingEdges

    assertEquals(incomingEdges.get(nodeA), None) // No incoming edges to A
    assertEquals(incomingEdges.get(nodeB), Some(List(nodeA)))
    assertEquals(incomingEdges.get(nodeC).map(_.toSet), Some(Set(nodeA, nodeB)))
  }

  test("reachableNodesFromSink - convenient overload") {
    val dag = new DAG[Double]()
    val nodeA: AdNode[Double] = DebugNode("A") // source
    val nodeB: AdNode[Double] = DebugNode("B") // intermediate
    val nodeC: AdNode[Double] = DebugNode("C") // sink

    dag.addNode(nodeA)
    dag.addNode(nodeB)
    dag.addNode(nodeC)
    dag.addEdge(nodeA, nodeB)
    dag.addEdge(nodeB, nodeC)

    val reachable = dag.reachableNodesFromSink(nodeC, Set(nodeA))
    assertEquals(reachable, Set(nodeA, nodeB, nodeC))
  }

end DAGSuite
