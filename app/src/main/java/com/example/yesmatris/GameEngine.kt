package com.example.yesmatris

// 1. Taşlara kimlik kazandıran sınıfımız. Compose artık taşları bu ID ile takip edecek!
data class Tile(val id: Int, var value: Int)

class GameEngine {
    // 2. IntArray yerine artık içinde kimlikli Tile nesneleri tutan bir matris kullanıyoruz
    var board = Array(4) { Array<Tile?>(4) { null } }

    var score = 0
        private set

    // Her yeni oluşan taşa benzersiz bir kimlik vereceğiz
    private var nextId = 1

    fun resetBoard() {
        board = Array(4) { Array<Tile?>(4) { null } }
        score = 0
        nextId = 1
        addRandomNumber()
        addRandomNumber()
    }

    fun addRandomNumber() {
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                if (board[i][j] == null) {
                    emptyCells.add(Pair(i, j))
                }
            }
        }
        if (emptyCells.isNotEmpty()) {
            val randomCell = emptyCells.random()
            val (row, col) = randomCell
            val value = if (Math.random() < 0.9) 2 else 4
            // Yeni taşa sıradaki ID'yi veriyoruz
            board[row][col] = Tile(id = nextId++, value = value)
        }
    }

    fun isGameOver(): Boolean {
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                if (board[i][j] == null) return false
            }
        }

        for (i in 0 until 4) {
            for (j in 0 until 3) {
                if (board[i][j]?.value == board[i][j + 1]?.value) return false
                if (board[j][i]?.value == board[j + 1][i]?.value) return false
            }
        }
        return true
    }

    fun swipeLeft(): Boolean {
        var moved = false
        for (i in 0 until 4) {
            val originalRow = board[i].clone()
            board[i] = slideAndMergeRow(board[i])
            if (!originalRow.contentEquals(board[i])) moved = true
        }
        if (moved) addRandomNumber()
        return moved
    }

    fun swipeRight(): Boolean {
        var moved = false
        for (i in 0 until 4) {
            val originalRow = board[i].clone()
            val reversedRow = originalRow.reversedArray()
            val mergedRow = slideAndMergeRow(reversedRow).reversedArray()
            board[i] = mergedRow
            if (!originalRow.contentEquals(board[i])) moved = true
        }
        return moved
    }
    fun swipeUp(): Boolean {
        var moved = false
        for (col in 0 until 4) {
            val originalCol = Array<Tile?>(4) { row -> board[row][col] }
            val mergedCol = slideAndMergeRow(originalCol)
            for (row in 0 until 4) {
                if (board[row][col] != mergedCol[row]) moved = true
                board[row][col] = mergedCol[row]
            }
        }
        return moved
    }
    fun swipeDown(): Boolean {
        var moved = false
        for (col in 0 until 4) {
            val originalCol = Array<Tile?>(4) { row -> board[row][col] }
            val reversedCol = originalCol.reversedArray()
            val mergedCol = slideAndMergeRow(reversedCol).reversedArray()
            for (row in 0 until 4) {
                if (board[row][col] != mergedCol[row]) moved = true
                board[row][col] = mergedCol[row]
            }
        }
        return moved    }

    private fun slideAndMergeRow(row: Array<Tile?>): Array<Tile?> {
        val nonNulls = row.filterNotNull().toMutableList()
        var i = 0
        while (i < nonNulls.size - 1) {
            if (nonNulls[i].value == nonNulls[i + 1].value) {
                // Çarpışan iki taştan ilkini koruyup sadece değerini güncelliyoruz.
                // Bu sayede Compose taşı yok etmek yerine kaydırıp sayısını artırıyor!
                nonNulls[i].value *= 2
                score += nonNulls[i].value
                nonNulls.removeAt(i + 1)
            }
            i++
        }
        val result = Array<Tile?>(4) { null }
        for (j in nonNulls.indices) {
            result[j] = nonNulls[j]
        }
        return result
    }
}