package mazerunner

import java.io.File
import kotlin.random.Random

class Cell(
    var isWall: Boolean = true,
    var prev: Pair<Int, Int>? = null,
    var neighbors: MutableList<Pair<Int, Int>> = mutableListOf(),
    var isVisited: Boolean = false
)

class MazeGenerator(var size: Int = 0, private var maze: Array<Array<Cell>> = arrayOf()) {
    private lateinit var entrance: Pair<Int, Int>
    private lateinit var exit: Pair<Int, Int>
    private var escapePath = mutableListOf<Pair<Int, Int>>()

    override fun toString(): String {
        var s = "\n"
        maze.forEach { line ->
            line.forEach { cell ->
                s += if (cell.isWall) "\u2588\u2588" else "  "
            }
            s += "\n"
        }
        return s
    }

    private fun connectCells(cellA: Pair<Int, Int>, cellB: Pair<Int, Int>) {
        maze[cellA.first][cellA.second].neighbors.add(cellB)
        maze[cellB.first][cellB.second].neighbors.add(cellA)
    }

    /**
     * return the frontier cells and the cells between them and @param coordinates.
     */
    private fun computeFrontierCells(coordinates: Pair<Int, Int>): MutableList<Pair<Pair<Int, Int>, Pair<Int, Int>>> {
        val newFrontierCells = mutableListOf(
            Pair(Pair(coordinates.first + 2, coordinates.second), Pair(coordinates.first + 1, coordinates.second)),
            Pair(Pair(coordinates.first - 2, coordinates.second), Pair(coordinates.first - 1, coordinates.second)),
            Pair(Pair(coordinates.first, coordinates.second + 2), Pair(coordinates.first, coordinates.second + 1)),
            Pair(Pair(coordinates.first, coordinates.second - 2), Pair(coordinates.first, coordinates.second - 1))
        )

        newFrontierCells.removeAll {
            it.first.first !in 1 until maze.size - 1
                    || it.first.second !in 1 until maze[0].size - 1
                    || !maze[it.first.first][it.first.second].isWall
                    || maze[it.first.first][it.first.second].neighbors.isNotEmpty()
        }
        //connect
        newFrontierCells.forEach {
            connectCells(it.first, it.second)
            connectCells(coordinates, it.second)
        }
        return newFrontierCells
    }

    fun generate() {
        //fill the whole maze with blocks
        maze = Array(size) { Array(size) { Cell() } }
        val random = Random.Default
        //pick random entrance
        entrance = Pair(random.nextInt(1, size - 1), 0)
        maze[entrance.first][entrance.second].isWall = false
        //initiate frontier cells
        val frontierCells = computeFrontierCells(entrance)
        while (frontierCells.size > 0) {
            //pick random frontier cell
            val randomCell = frontierCells.removeAt(random.nextInt(frontierCells.size))
            maze[randomCell.first.first][randomCell.first.second].isWall = false
            maze[randomCell.second.first][randomCell.second.second].isWall = false
            //compute frontier cells
            frontierCells.addAll(computeFrontierCells(randomCell.first))
        }
        //pick random exit
        val randomExitIndex = maze.indices.filter { !maze[it][size - 3].isWall }.random()
        maze[randomExitIndex][size - 1].isWall = false
        maze[randomExitIndex][size - 2].isWall = false
        connectCells(Pair(randomExitIndex, size -1), Pair(randomExitIndex, size -2))
        connectCells(Pair(randomExitIndex, size -3), Pair(randomExitIndex, size -2))
        exit = Pair(randomExitIndex, size - 1)
    }

    private fun hasValidFormat(): Boolean {
        //TODO
        return true
    }

    fun save(fileName: String) {
        val mazeFile = File(fileName)
        mazeFile.writeText("$size\n")
        maze.forEach { line ->
            line.forEach { cell ->
                mazeFile.appendText(cell.isWall.toString() + " ")
            }
            mazeFile.appendText("\n")
        }
    }

    private fun findEntrance(): Pair<Int, Int> {
        return Pair(maze.indexOfFirst { !it[0].isWall }, 0)
    }
    private fun findExit(): Pair<Int, Int> {
        return Pair(maze.indexOfFirst { !it[size-1].isWall }, size-1)
    }

    private fun connectPassage(current: Pair<Int, Int>, next: Pair<Int, Int>): Boolean{
        if (next.first in 0 until size && next.second in 0 until size) {
            if(maze[next.first][next.second].isWall)
                return false
            if(maze[current.first][current.second].neighbors.contains(next))
                return false
            connectCells(current, next)
            return true
        }
        return false
    }

    private fun reconnectGraph(passage: Pair<Int,Int>){
        if(connectPassage(passage, Pair(passage.first+1, passage.second)))
            reconnectGraph( Pair(passage.first+1, passage.second))
        if(connectPassage(passage, Pair(passage.first-1, passage.second)))
            reconnectGraph( Pair(passage.first-1, passage.second))
        if(connectPassage(passage, Pair(passage.first, passage.second+1)))
            reconnectGraph( Pair(passage.first, passage.second+1))
        if(connectPassage(passage, Pair(passage.first, passage.second-1)))
            reconnectGraph( Pair(passage.first, passage.second-1))
    }

    fun load(fileName: String): Boolean {
        val mazeFile = File(fileName)
        if (!mazeFile.exists()) {
            println("The file ... does not exist")
            return false
        }
        if (!hasValidFormat()) {
            println("Cannot load the maze. It has an invalid format")
            return false
        }
        mazeFile.readLines().forEachIndexed { index, s ->
            if (index == 0) {
                size = s.toInt()
                maze = Array(size) { Array(size) { Cell() } }
            } else {
                maze[index - 1] = s.split(" ").map { Cell(it.toBoolean()) }.slice(0 until size).toTypedArray()
            }
        }
        entrance = findEntrance()
        exit = findExit()
        reconnectGraph(entrance)
        escapePath.clear()
        return true
    }

    fun escape() {
        findRoute(entrance)
        traceRoute()
    }

    private fun traceRoute() {
        println()
        for (x in 0 until size) {
            for (y in 0 until size) {
                if (maze[x][y].isWall) {
                    print("\u2588\u2588")
                } else if (escapePath.contains(Pair(x, y)))
                    print("//")
                else
                    print("  ")
            }
            println()
        }
    }

    private fun findRoute(passage: Pair<Int, Int>) {
        maze[passage.first][passage.second].isVisited = true
        escapePath.clear()
        escapePath.add(passage)
        while (escapePath.isNotEmpty()) {
            val currentCell = escapePath.removeFirst()
            for(cell in maze[currentCell.first][currentCell.second].neighbors){
                if(!maze[cell.first][cell.second].isVisited){
                    maze[cell.first][cell.second].isVisited = true
                    escapePath.add(cell)
                    maze[cell.first][cell.second].prev = currentCell
                    if(cell == exit){
                        escapePath.clear()
                        break
                    }
                }
            }
        }

        var cell: Pair<Int, Int>? = exit
        while (cell!=null){
            escapePath.add(cell)
            cell = maze[cell.first][cell.second].prev
        }
    }
}

fun displayMenu(doMazeExist: Boolean) {
    println("1. Generate a new maze.\n2. Load a maze.")
    if (doMazeExist) println("3. Save the maze.\n4. Display the maze.\n5. Find the escape.")
    println("0. Exit.")
    print(">")
}

fun main() {
    var option = -1
    var doMazeExist = false
    val maze = MazeGenerator()
    do {
        displayMenu(doMazeExist)
        try {
            option = readln().toInt()
            when (option) {
                1 -> {
                    print("Please, enter the size of a maze\n>")
                    maze.size = readln().toInt()
                    maze.generate()
                    print(maze)
                    doMazeExist = true
                }

                2 -> {
                    print(">")
                    if (maze.load(readln())) {
                        doMazeExist = true
                    }

                }

                3 -> {
                    if (doMazeExist) {
                        print(">")
                        maze.save(readln())
                    } else {
                        println("Incorrect option. Please try again")
                    }
                }

                4 -> {
                    if (doMazeExist) {
                        print(maze)
                    } else {
                        println("Incorrect option. Please try again")
                    }
                }

                5 -> {
                    if (doMazeExist) {
                        maze.escape()
                    } else {
                        println("Incorrect option. Please try again")
                    }
                }

                0 -> {
                    println("Bye !")
                }

                else -> {
                    println("Incorrect option. Please try again")
                }
            }
        } catch (e: NumberFormatException) {
            println("Incorrect option. Please try again")
        }

    } while (option != 0)
}