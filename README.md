# Pac-Man Project – Custom Server & Algorithm Integration

## Overview
This project is a **full standalone implementation of the Pac-Man game server**, built entirely from scratch without relying on the lecturer’s provided executable (`exe.ex3`).

Course: Introduction to Computer Science Institution: Ariel University, School of Computer Science Year: 2026

The system preserves **full compatibility with the lecturer’s interface**, while replacing:
- The game engine
- Board handling
- Ghost logic
- Power mechanics
- GUI rendering

---

## Part 1 – Algorithm Development (Ex3Algo)

### Goal
The objective was to create an intelligent agent capable of maximizing score while navigating complex mazes and avoiding dynamic threats in real-time.

### Advanced Logic & Mechanics
The code implements several high-level strategies to handle edge cases and optimize performance:

* **Breadth-First Search (BFS) Engine:** The core of the movement logic. It calculates the shortest path to targets (`DOT`, `POWER`) while considering walls and "danger zones" created by ghosts.
* **Strategic Power Policy (Power Lock):** * To prevent wasting power pellets, the algorithm implements a "Lock": it avoids stepping on `GREEN` pellets if it's already in Power Mode or during the first 5 seconds of the game (`NO_POWER_FIRST_TICKS`).
* **Heuristic Scoring & Tie-Breaking:** When multiple targets are at the same distance, the algorithm scores them based on:
    * **Threat Level:** Distance to the nearest danger ghost.
    * **Exits:** Favoring tiles with more escape routes to avoid traps.
    * **Directional Bias:** Prefers continuing in the current direction to maintain momentum.
* **Loop & Stuck Prevention:** * **Position Memory:** Uses an `ArrayDeque` to store recent positions. If it detects a loop, it forces a different legal move.
    * **Anti-Reverse:** Prevents Pac-Man from oscillating between two tiles by penalizing immediate 180-degree turns.
* **Ghost Hunting (Chase Mode):** In Power Mode, it calculates if an eatable ghost is reachable before the timer expires, using a safety margin (`EATABLE_TIME_MARGIN`).

---

##  Part 2 – `my_game` Package (Custom Server Implementation)

The `my_game` package contains the **entire Pac-Man server**, implemented independently.

---

##  MyPacmanGame – Core Game Server

### Responsibilities
`MyPacmanGame` is the **authoritative game server**.  
It is responsible for:

- Managing the board (`int[][]`)
- Tracking Pac-Man position and movement
- Managing ghost behavior and state
- Handling power pellets and eatable mode
- Detecting collisions and scoring
- Enforcing cyclic map logic
- Communicating with the algorithm

### Game Loop (High-Level)

### Cyclic Maps
- Leaving the board on one side wraps the entity to the opposite side
- Implemented via `wrapX()` and `wrapY()`
- Fully transparent to the algorithm

---

## MyGhost – Ghost Logic 

Each ghost:
- Maintains its own position and state
- Has a movement strategy:
  - `RANDOM_WALK`
  - `GREEDY_SP` (chases Pac-Man)
- Can become **eatable** after a power pellet
- Can be **eaten**, temporarily removed, and respawned

### Intelligent Behavior
- Ghosts never move through walls
- Greedy ghosts minimize Manhattan distance to Pac-Man
- Some ghosts remain random to avoid deterministic patterns

---

## MyGameMap – Board Construction

### Purpose
Separates **map creation** from **game logic**.

### Features
- Builds the board from ASCII maps
- Automatically pads uneven rows with walls
- Converts symbols:
  - `#` → WALL
  - `.` → DOT
  - `o` → POWER
- Reverses Y-axis so that `UP` increases Y (algorithm-compatible)

This allows easy creation of new levels without modifying engine logic.

---

## MyGui – Visualization Layer

### Design Philosophy
The GUI acts as a **pure client**:
- Reads game state via `PacManGame`
- Never modifies game logic
- Mimics real server-client separation

### Features
- Blue walls
- Pink dots and green power pellets
- Pac-Man rotates according to movement direction
- Eatable ghosts rendered as red circles
- Eaten ghosts temporarily disappear

---

## Game.java – Compatibility Utility

Replaces `exe.ex3.game.Game`:
- Direction constants
- Color-to-int mappings
- Ensures seamless interaction with `Ex3Algo`

---

## MyMain – Program Entry Point

Responsibilities:
- Initialize the game server
- Load a level
- Attach the algorithm
- Run the game loop
- Render the GUI
- Print final results

---

## Summary

✔ Fully standalone Pac-Man server  
✔ Algorithm-compatible without modification  
✔ Clean OOP design and separation of concerns  
✔ Cyclic maps, power pellets, ghost 
✔ Server-authoritative architecture  

This project demonstrates:
- Interface-driven design
- Game engine architecture
- Clean separation between server, algorithm, and GUI

---
