package io.github.quafadas.spireAD

import scala.collection.mutable
import java.util.UUID
import java.util as ju
import algebra.ring.Field
import spire.algebra.Trig
import scala.reflect.ClassTag
import spire.algebra.NRoot

class DAG[T: Field: ClassTag: Trig: NRoot]:
  private val adjacencyList: mutable.Map[UUID, mutable.Set[UUID]] =
    mutable.Map.empty
  private val nodeMap: mutable.Map[UUID, AdNode[T]] = mutable.Map.empty

  inline def getAllNodes: Set[AdNode[T]] =
    nodeMap.values.toSet

  inline def getAllEdges: Set[(AdNode[T], AdNode[T])] =
    adjacencyList.flatMap { case (fromId, toIds) =>
      toIds.map(toId => (nodeMap(fromId), nodeMap(toId)))
    }.toSet

  inline def getNode(id: UUID): AdNode[T] =
    nodeMap.getOrElse(
      id,
      throw new NoSuchElementException(s"Node with id $id not found")
    )

  inline def addAdNode(adNode: AdNode[T]): Unit =
    if !adjacencyList.contains(adNode.id) then
      adjacencyList(adNode.id) = mutable.Set.empty
      nodeMap(adNode.id) = adNode

  inline def addNode(adNode: AdNode[T]): Unit =
    addAdNode(adNode)

  inline def addStringNode(str: String): Unit =
    val adNode = DebugNode(str)
    addAdNode(adNode)
  end addStringNode

  inline def addTejNode(tej: Tej[T]): Unit =
    val adNode = TejNode(tej)
    addAdNode(adNode)
  end addTejNode

  inline def addOpNodeUrnary(
      op: UrnaryOps,
      t: Tej[T],
      dep: UUID
  ): Unit =
    val adNode = TejOpUrnary(op, t, dep)
    addAdNode(adNode)
  end addOpNodeUrnary

  inline def addEdge[T](from: AdNode[T], to: AdNode[T]): Unit =
    require(adjacencyList.contains(from.id), s"AdNode $from does not exist.")
    require(adjacencyList.contains(to.id), s"AdNode $to does not exist.")
    // println(s"Adding edge from ${from} to ${to}")
    adjacencyList(from.id) += to.id
  end addEdge

  inline def addTedge(from: Tej[T], to: Tej[T]): Unit =
    val fromNode = TejNode(from)
    val toNode = TejNode(to)
    addEdge(toNode, fromNode)
  end addTedge

  inline def addUrnaryEdge(
      from: Tej[T],
      to: TejOpUrnary[T]
  ): Unit =
    val fromNode = TejNode(from)
    addEdge(to, fromNode)
  end addUrnaryEdge

  inline def addBinaryEdge(
      left: Tej[T],
      right: Tej[T],
      to: TejOpBinary[T]
  ): Unit =
    val fromNode = TejNode(to.value)
    addEdge(to, TejNode(left))
    addEdge(to, TejNode(right))
  end addBinaryEdge

  inline def addSedge[T](from: String, to: String): Unit =
    val fromNode = DebugNode(from)
    val toNode = DebugNode(to)
    addEdge(fromNode, toNode)
  end addSedge

  inline def removeNode(adNode: AdNode[T]): Unit =
    adjacencyList -= adNode.id
    nodeMap -= adNode.id
    adjacencyList.values.foreach(_ -= adNode.id)
  end removeNode

  inline def removeEdge(from: AdNode[T], to: AdNode[T]): Unit =
    adjacencyList.get(from.id).foreach(_ -= to.id)

  inline def removeSedge(from: String, to: String): Unit =
    val fromNode = DebugNode(from)
    val toNode = DebugNode(to)
    adjacencyList.get(fromNode.id).foreach(_ -= toNode.id)
  end removeSedge

  inline def neighbors(adNode: AdNode[T]): Set[AdNode[T]] =
    adjacencyList.getOrElse(adNode.id, Set.empty).flatMap(nodeMap.get).toSet

  inline def hasEdge(from: AdNode[T], to: AdNode[T]): Boolean =
    adjacencyList.get(from.id).exists(_.contains(to.id))

  inline def isEmpty: Boolean = adjacencyList.isEmpty

  def toposort: List[AdNode[T]] =
    val adj = mutable.Map.empty[AdNode[T], mutable.ListBuffer[AdNode[T]]]
    val indegree = mutable.Map.empty[AdNode[T], Int].withDefaultValue(0)

    // Build adjacency list and indegree map
    for (from, to) <- getAllEdges do
      adj.getOrElseUpdate(from, mutable.ListBuffer()) += to
      indegree(to) += 1
      adj.getOrElseUpdate(
        to,
        mutable.ListBuffer()
      ) // Ensure all nodes are in adj
    end for

    // Initialize queue with nodes having indegree 0
    val q = mutable.Queue[AdNode[T]]()
    for node <- adj.keys if indegree(node) == 0 do q.enqueue(node)
    end for

    val result = mutable.ListBuffer[AdNode[T]]()
    while q.nonEmpty do
      val node = q.dequeue()
      result += node

      for neighbor <- adj(node) do
        indegree(neighbor) -= 1
        if indegree(neighbor) == 0 then q.enqueue(neighbor)
        end if
      end for
    end while

    // Check for cycle
    if result.size != adj.size then throw new IllegalArgumentException("Graph contains cycle!")
    end if

    result.toList
  end toposort

  inline def toGraphviz: String =
    val sb = new StringBuilder
    sb.append("digraph {\n")

    adjacencyList.foreach { case (node, neighbors) =>
      if neighbors.isEmpty then sb.append(s"  \"${graphShow(nodeMap(node))}\";\n")
      else
        neighbors.foreach { neighbor =>
          sb.append(
            s"  \"${graphShow(nodeMap(node))}\" -> \"${graphShow(nodeMap(neighbor))}\";\n"
          )
        }
    }

    sb.append("}")
    sb.toString()
  end toGraphviz
end DAG
