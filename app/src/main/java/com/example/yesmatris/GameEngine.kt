package com.example.yesmatris

class GameEngine {
    // 4x4'lük oyun matrisimiz (grid). Başlangıçta her hücre 0 (boş) olarak ayarlanıyor.
    var board = Array(4) { IntArray(4) { 0 } }

    // Oyunu başlatan veya sıfırlayan ana fonksiyon
    fun resetBoard() {
        // Tahtayı tamamen sıfırla
        board = Array(4) { IntArray(4) { 0 } }

        // Oyun başlarken rastgele 2 adet sayı (2 veya 4) ekliyoruz
        addRandomNumber()
        addRandomNumber()
    }

    // Tahtadaki boş (0 olan) hücrelerden birine rastgele 2 veya 4 ekleyen fonksiyon
    fun addRandomNumber() {
        val emptyCells = mutableListOf<Pair<Int, Int>>()

        // Matrisi tarayıp boş olan (0 olan) hücrelerin x ve y koordinatlarını buluyoruz
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                if (board[i][j] == 0) {
                    emptyCells.add(Pair(i, j))
                }
            }
        }

        // Eğer boş hücre varsa, rastgele birini seç
        if (emptyCells.isNotEmpty()) {
            val randomCell = emptyCells.random()
            val (row, col) = randomCell

            // %90 ihtimalle 2, %10 ihtimalle 4 koy (2048'in klasik kuralı)
            board[row][col] = if (Math.random() < 0.9) 2 else 4
        }
    }

    // Sola kaydırma
    fun swipeLeft() {
        var moved = false
        for (i in 0 until 4) {
            val originalRow = board[i].clone()
            board[i] = slideAndMergeRow(board[i])
            if (!originalRow.contentEquals(board[i])) moved = true
        }
        // Eğer tahtada bir hareket olduysa yeni sayı ekle
        if (moved) addRandomNumber()
    }

    // Sağa kaydırma (Diziyi ters çevirip sola kaydırır gibi işlem yapıyoruz, sonra tekrar düzeltiyoruz)
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

    // Yukarı kaydırma (Sütunları satır gibi alıp işliyoruz)
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

    // Aşağı kaydırma
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

    // Çekirdek Birleştirme Algoritması (Verilen bir diziyi sola doğru kaydırır ve aynıları toplar)
    private fun slideAndMergeRow(row: IntArray): IntArray {
        // 1. Adım: Tüm 0'ları aradan çıkar, sadece dolu sayıları yan yana diz
        val nonZeroes = row.filter { it != 0 }.toMutableList()

        // 2. Adım: Yan yana duran aynı sayıları topla
        var i = 0
        while (i < nonZeroes.size - 1) {
            if (nonZeroes[i] == nonZeroes[i + 1]) {
                nonZeroes[i] *= 2 // Sayıyı ikiye katla
                nonZeroes.removeAt(i + 1) // İkinci sayıyı sil
            }
            i++
        }

        // 3. Adım: Dizinin sonunu tekrar 0'larla doldurarak boyutu 4'e tamamla
        val result = IntArray(4) { 0 }
        for (j in nonZeroes.indices) {
            result[j] = nonZeroes[j]
        }
        return result
    }
}