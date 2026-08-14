package com.example.ui.simulator

import android.content.Context
import android.content.Intent
import android.os.Build
import java.io.File

object LocalServerManager {
    var server: LocalServer? = null
    var serverPort: Int = 0 

    fun startServer(context: Context) {
        val serviceIntent = Intent(context, LocalServerService::class.java).apply {
            action = LocalServerService.ACTION_START_SERVER
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Also ensure the server instance itself is started locally immediately
        startLocalInstance(context)
    }

    fun startLocalInstance(context: Context) {
        initializeOfflineApps(context)
        if (server == null) {
            val rootFolder = context.filesDir
            var currentPort = 8080
            while (server == null && currentPort < 8100) {
                try {
                    server = LocalServer(rootFolder, currentPort)
                    serverPort = currentPort
                } catch (e: Exception) {
                    currentPort++
                }
            }
        }
    }

    private fun initializeOfflineApps(context: Context) {
        val baseDir = File(context.filesDir, "kaios_apps")
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }

        // 1. Classic Snake
        val snakeDir = File(baseDir, "snake")
        if (!snakeDir.exists() || !File(snakeDir, "index.html").exists()) {
            snakeDir.mkdirs()
            File(snakeDir, "manifest.webapp").writeText("""
                {
                  "name": "Classic Snake",
                  "description": "Feed the snake and avoid the walls inside the classic retro offline game.",
                  "launch_path": "/index.html",
                  "icons": {
                    "56": "/icon.png",
                    "128": "/icon.png"
                  },
                  "developer": {
                    "name": "KaiOS Retro"
                  }
                }
            """.trimIndent())
            
            File(snakeDir, "index.html").writeText("""
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                  <title>Snake Game</title>
                  <style>
                    body {
                      margin: 0;
                      background: #000;
                      color: #fff;
                      font-family: monospace;
                      text-align: center;
                      overflow: hidden;
                    }
                    #game-container {
                      position: relative;
                      width: 100vw;
                      height: 100vh;
                      display: flex;
                      flex-direction: column;
                      justify-content: space-between;
                      align-items: center;
                      box-sizing: border-box;
                      padding: 10px;
                    }
                    canvas {
                      border: 2px solid #333;
                      background: #111;
                      image-rendering: pixelated;
                    }
                    .hud {
                      display: flex;
                      justify-content: space-between;
                      width: 100%;
                      font-size: 14px;
                    }
                    .status {
                      font-size: 18px;
                      color: #ff9800;
                      font-weight: bold;
                    }
                    .footer {
                      font-size: 10px;
                      color: #888;
                    }
                  </style>
                </head>
                <body>
                  <div id="game-container">
                    <div class="hud">
                      <div>SCORE: <span id="score">0</span></div>
                      <div>HIGH: <span id="highscore">0</span></div>
                    </div>
                    <div id="status" class="status">PRESS [ENTER] TO START</div>
                    <canvas id="gameCanvas" width="220" height="220"></canvas>
                    <div class="footer">Use DPAD / ARROWS to turn</div>
                  </div>

                  <script>
                    const canvas = document.getElementById("gameCanvas");
                    const ctx = canvas.getContext("2d");
                    const scoreEl = document.getElementById("score");
                    const highscoreEl = document.getElementById("highscore");
                    const statusEl = document.getElementById("status");

                    const gridSize = 10;
                    const tileCount = 22;
                    let snake = [{x: 11, y: 11}];
                    let food = {x: 5, y: 5};
                    let dx = 0;
                    let dy = 0;
                    let score = 0;
                    let highscore = localStorage.getItem("snake_high") || 0;
                    highscoreEl.innerText = highscore;
                    let gameInterval = null;
                    let isPlaying = false;

                    function draw() {
                      ctx.fillStyle = "#111";
                      ctx.fillRect(0, 0, canvas.width, canvas.height);

                      ctx.fillStyle = "#4CAF50";
                      snake.forEach((part, index) => {
                        if (index === 0) ctx.fillStyle = "#8BC34A";
                        else ctx.fillStyle = "#4CAF50";
                        ctx.fillRect(part.x * gridSize, part.y * gridSize, gridSize - 1, gridSize - 1);
                      });

                      ctx.fillStyle = "#FF5722";
                      ctx.fillRect(food.x * gridSize, food.y * gridSize, gridSize - 1, gridSize - 1);
                    }

                    function move() {
                      if (dx === 0 && dy === 0) return;

                      const head = {x: snake[0].x + dx, y: snake[0].y + dy};

                      if (head.x < 0 || head.x >= tileCount || head.y < 0 || head.y >= tileCount) {
                        gameOver();
                        return;
                      }

                      for (let i = 0; i < snake.length; i++) {
                        if (snake[i].x === head.x && snake[i].y === head.y) {
                          gameOver();
                          return;
                        }
                      }

                      snake.unshift(head);

                      if (head.x === food.x && head.y === food.y) {
                        score += 10;
                        scoreEl.innerText = score;
                        if (score > highscore) {
                          highscore = score;
                          highscoreEl.innerText = highscore;
                          localStorage.setItem("snake_high", highscore);
                        }
                        generateFood();
                      } else {
                        snake.pop();
                      }
                    }

                    function generateFood() {
                      food.x = Math.floor(Math.random() * tileCount);
                      food.y = Math.floor(Math.random() * tileCount);
                      snake.forEach(part => {
                        if (part.x === food.x && part.y === food.y) {
                          generateFood();
                        }
                      });
                    }

                    function tick() {
                      move();
                      draw();
                    }

                    function startGame() {
                      if (isPlaying) return;
                      snake = [{x: 11, y: 11}];
                      dx = 1;
                      dy = 0;
                      score = 0;
                      scoreEl.innerText = score;
                      generateFood();
                      statusEl.innerText = "PLAYING";
                      isPlaying = true;
                      if (gameInterval) clearInterval(gameInterval);
                      gameInterval = setInterval(tick, 150);
                    }

                    function gameOver() {
                      clearInterval(gameInterval);
                      isPlaying = false;
                      statusEl.innerText = "GAME OVER! [ENTER] RESTART";
                      dx = 0;
                      dy = 0;
                    }

                    window.addEventListener("keydown", function(e) {
                      if (!isPlaying && (e.key === "Enter" || e.keyCode === 13)) {
                        startGame();
                        return;
                      }

                      switch (e.key) {
                        case "ArrowUp":
                        case "Up":
                          if (dy !== 1) { dx = 0; dy = -1; }
                          break;
                        case "ArrowDown":
                        case "Down":
                          if (dy !== -1) { dx = 0; dy = 1; }
                          break;
                        case "ArrowLeft":
                        case "Left":
                          if (dx !== 1) { dx = -1; dy = 0; }
                          break;
                        case "ArrowRight":
                        case "Right":
                          if (dx !== -1) { dx = 1; dy = 0; }
                          break;
                      }
                    });

                    draw();
                  </script>
                </body>
                </html>
            """.trimIndent())
        }

        // 2. Calculator
        val calcDir = File(baseDir, "calculator")
        if (!calcDir.exists() || !File(calcDir, "index.html").exists()) {
            calcDir.mkdirs()
            File(calcDir, "manifest.webapp").writeText("""
                {
                  "name": "Calculator",
                  "description": "Perform basic arithmetic calculations offline with ease.",
                  "launch_path": "/index.html",
                  "icons": {
                    "56": "/icon.png",
                    "128": "/icon.png"
                  },
                  "developer": {
                    "name": "KaiOS Systems"
                  }
                }
            """.trimIndent())
            
            File(calcDir, "index.html").writeText("""
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                  <title>Calculator</title>
                  <style>
                    body {
                      margin: 0;
                      background: #202020;
                      color: #fff;
                      font-family: sans-serif;
                      overflow: hidden;
                    }
                    #calc {
                      display: flex;
                      flex-direction: column;
                      height: 100vh;
                      justify-content: space-between;
                      box-sizing: border-box;
                      padding: 10px;
                    }
                    #screen {
                      background: #111;
                      border: 1px solid #333;
                      border-radius: 4px;
                      padding: 10px;
                      height: 70px;
                      display: flex;
                      flex-direction: column;
                      justify-content: flex-end;
                      align-items: flex-end;
                      overflow: hidden;
                    }
                    #expr {
                      font-size: 14px;
                      color: #aaa;
                      height: 18px;
                    }
                    #result {
                      font-size: 26px;
                      font-weight: bold;
                      color: #4CAF50;
                    }
                    #grid {
                      display: grid;
                      grid-template-columns: repeat(4, 1fr);
                      grid-gap: 5px;
                      flex-grow: 1;
                      margin-top: 10px;
                    }
                    .btn {
                      background: #333;
                      border: none;
                      border-radius: 4px;
                      color: #fff;
                      font-size: 16px;
                      font-weight: bold;
                      display: flex;
                      justify-content: center;
                      align-items: center;
                    }
                    .btn.active {
                      background: #E91E63;
                    }
                    .footer {
                      font-size: 10px;
                      color: #888;
                      text-align: center;
                      margin-top: 4px;
                    }
                  </style>
                </head>
                <body>
                  <div id="calc">
                    <div id="screen">
                      <div id="expr"></div>
                      <div id="result">0</div>
                    </div>
                    <div id="grid">
                      <div class="btn" id="btn-7">7</div>
                      <div class="btn" id="btn-8">8</div>
                      <div class="btn" id="btn-9">9</div>
                      <div class="btn" id="btn-div" style="background:#555">/</div>
                      <div class="btn" id="btn-4">4</div>
                      <div class="btn" id="btn-5">5</div>
                      <div class="btn" id="btn-6">6</div>
                      <div class="btn" id="btn-mul" style="background:#555">*</div>
                      <div class="btn" id="btn-1">1</div>
                      <div class="btn" id="btn-2">2</div>
                      <div class="btn" id="btn-3">3</div>
                      <div class="btn" id="btn-sub" style="background:#555">-</div>
                      <div class="btn" id="btn-0">0</div>
                      <div class="btn" id="btn-dot">.</div>
                      <div class="btn" id="btn-eq" style="background:#4CAF50">=</div>
                      <div class="btn" id="btn-add" style="background:#555">+</div>
                    </div>
                    <div class="footer">Use Numpad & DPAD (/, *, -, +) & [ENTER]</div>
                  </div>

                  <script>
                    const exprEl = document.getElementById("expr");
                    const resultEl = document.getElementById("result");
                    let expr = "";
                    let currentInput = "0";

                    function updateScreen() {
                      exprEl.innerText = expr;
                      resultEl.innerText = currentInput;
                    }

                    function handleInput(key) {
                      if (key >= '0' && key <= '9') {
                        if (currentInput === "0") {
                          currentInput = key;
                        } else {
                          currentInput += key;
                        }
                      } else if (key === '.') {
                        if (!currentInput.includes('.')) {
                          currentInput += '.';
                        }
                      } else if (key === 'Backspace' || key === 'Delete') {
                        if (currentInput.length > 1) {
                          currentInput = currentInput.slice(0, -1);
                        } else {
                          currentInput = "0";
                        }
                      } else if (key === '+' || key === '-' || key === '*' || key === '/') {
                        expr = currentInput + " " + key + " ";
                        currentInput = "0";
                      } else if (key === 'Enter' || key === '=') {
                        if (expr) {
                          try {
                            const finalExpr = expr + currentInput;
                            const res = eval(finalExpr);
                            currentInput = String(res);
                            expr = "";
                          } catch(e) {
                            currentInput = "Error";
                          }
                        }
                      }
                      updateScreen();
                    }

                    window.addEventListener("keydown", function(e) {
                      let key = e.key;
                      if (key === "ArrowUp" || key === "Up") key = "+";
                      if (key === "ArrowDown" || key === "Down") key = "-";
                      if (key === "ArrowLeft" || key === "Left") key = "*";
                      if (key === "ArrowRight" || key === "Right") key = "/";
                      handleInput(key);

                      let btnId = "";
                      if (key >= '0' && key <= '9') btnId = "btn-" + key;
                      else if (key === '+') btnId = "btn-add";
                      else if (key === '-') btnId = "btn-sub";
                      else if (key === '*') btnId = "btn-mul";
                      else if (key === '/') btnId = "btn-div";
                      else if (key === '=') btnId = "btn-eq";
                      else if (key === 'Enter') btnId = "btn-eq";

                      if (btnId) {
                        const btn = document.getElementById(btnId);
                        if (btn) {
                          btn.classList.add("active");
                          setTimeout(() => btn.classList.remove("active"), 100);
                        }
                      }
                    });
                  </script>
                </body>
                </html>
            """.trimIndent())
        }

        // 3. Block Tetris
        val tetrisDir = File(baseDir, "tetris")
        if (!tetrisDir.exists() || !File(tetrisDir, "index.html").exists()) {
            tetrisDir.mkdirs()
            File(tetrisDir, "manifest.webapp").writeText("""
                {
                  "name": "Block Tetris",
                  "description": "Clear lines in this classic falling block retro game.",
                  "launch_path": "/index.html",
                  "icons": {
                    "56": "/icon.png",
                    "128": "/icon.png"
                  },
                  "developer": {
                    "name": "KaiOS Retro"
                  }
                }
            """.trimIndent())
            
            File(tetrisDir, "index.html").writeText("""
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                  <title>Tetris</title>
                  <style>
                    body {
                      margin: 0;
                      background: #111;
                      color: #fff;
                      font-family: monospace;
                      text-align: center;
                      overflow: hidden;
                    }
                    #game-container {
                      display: flex;
                      flex-direction: column;
                      align-items: center;
                      height: 100vh;
                      box-sizing: border-box;
                      padding: 5px;
                    }
                    #hud {
                      display: flex;
                      justify-content: space-between;
                      width: 100%;
                      max-width: 200px;
                      font-size: 14px;
                      margin-bottom: 5px;
                    }
                    canvas {
                      border: 2px solid #555;
                      background: #000;
                    }
                    #status {
                      color: #E91E63;
                      font-weight: bold;
                      margin-bottom: 5px;
                    }
                    .footer {
                      font-size: 10px;
                      color: #888;
                      margin-top: 5px;
                    }
                  </style>
                </head>
                <body>
                  <div id="game-container">
                    <div id="hud">
                      <div>SCORE: <span id="score">0</span></div>
                    </div>
                    <div id="status">PRESS [ENTER] TO PLAY</div>
                    <canvas id="tetris" width="160" height="240"></canvas>
                    <div class="footer">Left/Right/Down, [ENTER] Rotate</div>
                  </div>

                  <script>
                    const canvas = document.getElementById("tetris");
                    const ctx = canvas.getContext("2d");
                    const scoreEl = document.getElementById("score");
                    const statusEl = document.getElementById("status");

                    const ROWS = 15;
                    const COLS = 10;
                    const BLOCK_SIZE = 16;

                    let score = 0;
                    let board = Array.from({ length: ROWS }, () => Array(COLS).fill(0));
                    let isPlaying = false;
                    let gameInterval = null;

                    const SHAPES = [
                      [[1, 1, 1, 1]],
                      [[1, 1, 1], [0, 1, 0]],
                      [[1, 1, 0], [0, 1, 1]],
                      [[0, 1, 1], [1, 1, 0]],
                      [[1, 1], [1, 1]],
                      [[1, 1, 1], [1, 0, 0]],
                      [[1, 1, 1], [0, 0, 1]]
                    ];

                    const COLORS = ["#00FFFF", "#FF00FF", "#FF0000", "#00FF00", "#FFFF00", "#FFA500", "#0000FF"];

                    let currentPiece = null;
                    let currentPieceColor = "";
                    let pieceX = 0;
                    let pieceY = 0;

                    function newPiece() {
                      const idx = Math.floor(Math.random() * SHAPES.length);
                      currentPiece = SHAPES[idx];
                      currentPieceColor = COLORS[idx];
                      pieceX = Math.floor((COLS - currentPiece[0].length) / 2);
                      pieceY = 0;

                      if (checkCollision(0, 0, currentPiece)) {
                        gameOver();
                      }
                    }

                    function checkCollision(ox, oy, piece) {
                      for (let r = 0; r < piece.length; r++) {
                        for (let c = 0; c < piece[r].length; c++) {
                          if (piece[r][c]) {
                            const nextX = pieceX + c + ox;
                            const nextY = pieceY + r + oy;
                            if (nextX < 0 || nextX >= COLS || nextY >= ROWS) return true;
                            if (nextY >= 0 && board[nextY][nextX]) return true;
                          }
                        }
                      }
                      return false;
                    }

                    function merge() {
                      for (let r = 0; r < currentPiece.length; r++) {
                        for (let c = 0; c < currentPiece[r].length; c++) {
                          if (currentPiece[r][c]) {
                            board[pieceY + r][pieceX + c] = currentPieceColor;
                          }
                        }
                      }
                    }

                    function clearLines() {
                      let linesCleared = 0;
                      for (let r = ROWS - 1; r >= 0; r--) {
                        if (board[r].every(val => val !== 0)) {
                          board.splice(r, 1);
                          board.unshift(Array(COLS).fill(0));
                          linesCleared++;
                          r++;
                        }
                      }
                      if (linesCleared > 0) {
                        score += linesCleared * 100;
                        scoreEl.innerText = score;
                      }
                    }

                    function rotate() {
                      const rotated = Array.from({ length: currentPiece[0].length }, (_, c) =>
                        Array.from({ length: currentPiece.length }, (_, r) => currentPiece[currentPiece.length - 1 - r][c])
                      );
                      if (!checkCollision(0, 0, rotated)) {
                        currentPiece = rotated;
                      }
                    }

                    function moveDown() {
                      if (!checkCollision(0, 1, currentPiece)) {
                        pieceY++;
                      } else {
                        merge();
                        clearLines();
                        newPiece();
                      }
                    }

                    function draw() {
                      ctx.fillStyle = "#000";
                      ctx.fillRect(0, 0, canvas.width, canvas.height);

                      for (let r = 0; r < ROWS; r++) {
                        for (let c = 0; c < COLS; c++) {
                          if (board[r][c]) {
                            ctx.fillStyle = board[r][c];
                            ctx.fillRect(c * BLOCK_SIZE, r * BLOCK_SIZE, BLOCK_SIZE - 1, BLOCK_SIZE - 1);
                          }
                        }
                      }

                      if (currentPiece) {
                        ctx.fillStyle = currentPieceColor;
                        for (let r = 0; r < currentPiece.length; r++) {
                          for (let c = 0; c < currentPiece[r].length; c++) {
                            if (currentPiece[r][c]) {
                              ctx.fillRect((pieceX + c) * BLOCK_SIZE, (pieceY + r) * BLOCK_SIZE, BLOCK_SIZE - 1, BLOCK_SIZE - 1);
                            }
                          }
                        }
                      }
                    }

                    function tick() {
                      moveDown();
                      draw();
                    }

                    function startGame() {
                      board = Array.from({ length: ROWS }, () => Array(COLS).fill(0));
                      score = 0;
                      scoreEl.innerText = score;
                      statusEl.innerText = "PLAYING";
                      isPlaying = true;
                      newPiece();
                      if (gameInterval) clearInterval(gameInterval);
                      gameInterval = setInterval(tick, 600);
                      draw();
                    }

                    function gameOver() {
                      clearInterval(gameInterval);
                      isPlaying = false;
                      statusEl.innerText = "GAME OVER! [ENTER] RESTART";
                    }

                    window.addEventListener("keydown", function(e) {
                      if (!isPlaying && (e.key === "Enter" || e.keyCode === 13)) {
                        startGame();
                        return;
                      }
                      if (!isPlaying) return;

                      switch (e.key) {
                        case "ArrowLeft":
                        case "Left":
                          if (!checkCollision(-1, 0, currentPiece)) pieceX--;
                          break;
                        case "ArrowRight":
                        case "Right":
                          if (!checkCollision(1, 0, currentPiece)) pieceX++;
                          break;
                        case "ArrowDown":
                        case "Down":
                          moveDown();
                          break;
                        case "ArrowUp":
                        case "Up":
                        case "Enter":
                          rotate();
                          break;
                      }
                      draw();
                    });
                  </script>
                </body>
                </html>
            """.trimIndent())
        }
    }

    fun stopServer() {
        server?.stop()
        server = null
    }
}
