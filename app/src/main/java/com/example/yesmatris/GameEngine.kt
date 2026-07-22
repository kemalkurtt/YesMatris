package com.example.yesmatris

class GameEngine {
    var board = Array(4) { IntArray(4) { 0 } }

    // Anlık skor değişkenimiz
    var score = 0
        private set

    fun resetBoard() {
        board = Array(4) { IntArray(4) { 0 } }
        score = 0 // Yeni oyunda skor sıfırlanır
        addRandomNumber()
        addRandomNumber()
    }

    fun addRandomNumber() {
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                if (board[i][j] == 0) {
                    emptyCells.add(Pair(i, j))
                }
            }
        }
        if (emptyCells.isNotEmpty()) {
            val randomCell = emptyCells.random()
            val (row, col) = randomCell
            board[row][col] = if (Math.random() < 0.9) 2 else 4
        }
    }

    fun swipeLeft() {
        var moved = false
        for (i in 0 until 4) {
            val originalRow = board[i].clone()
            board[i] = slideAndMergeRow(board[i])
            if (!originalRow.contentEquals(board[i])) moved = true
        }
        if (moved) addRandomNumber()
    }

    fun swipeRight() {
        var moved = false
        for (i in 0 until 4) {
            val originalRow = board[i].clone()
            val reversedRow = originalRow.reversedArray()
            val mergedRow = slideAndMergeRow(reversedRow).reversedArray()
            board[i] = mergedRow
            if (!originalRow.contentEquals(board[i])) moved = true
        }
        if (moved) addRandomNumber()
    }

    fun swipeUp() {
        var moved = false
        for (col in 0 until 4) {
            val originalCol = IntArray(4) { row -> board[row][col] }
            val mergedCol = slideAndMergeRow(originalCol)
            for (row in 0 until 4) {
                if (board[row][col] != mergedCol[row]) moved = true
                board[row][col] = mergedCol[row]
            }
        }
        if (moved) addRandomNumber()
    }

    fun swipeDown() {
        var moved = false
        for (col in 0 until 4) {
            val originalCol = IntArray(4) { row -> board[row][col] }
            val reversedCol = originalCol.reversedArray()
            val mergedCol = slideAndMergeRow(reversedCol).reversedArray()
            for (row in 0 until 4) {
                if (board[row][col] != mergedCol[row]) moved = true
                board[row][col] = mergedCol[row]
            }
        }
        if (moved) addRandomNumber()
    }

    private fun slideAndMergeRow(row: IntArray): IntArray {
        val nonZeroes = row.filter { it != 0 }.toMutableList()
        var i = 0
        while (i < nonZeroes.size - 1) {
            if (nonZeroes[i] == nonZeroes[i + 1]) {
                nonZeroes[i] *= 2
                score += nonZeroes[i] // Birleşen yeni değer skora ekleniyor!
                nonZeroes.removeAt(i + 1)
            }
            i++
        }
        val result = IntArray(4) { 0 }
        for (j in nonZeroes.indices) {
            result[j] = nonZeroes[j]
        }
        return result
    }
}