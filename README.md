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
Design a Pac-Man agent that **maximizes score by eating DOTs efficiently**, while responding correctly to nearby ghosts and avoiding infinite loops or corner-sticking.

---

### Core Strategy
The algorithm runs once per game tick and chooses the next move using **Breadth-First Search (BFS) distance maps** combined with a **heuristic scoring function** over the four possible directions.

---

### Distance Maps (BFS Engine)
Each tick, the algorithm builds three multi-source BFS maps:

- **DOT Distance (`dotDist`)** – shortest path to the nearest pink DOT  
- **POWER Distance (`powDist`)** – shortest path to the nearest green POWER  
- **Danger Distance (`dangerDist`)** – shortest path to the nearest *danger* ghost  
  (ghosts with `remainTimeAsEatable(code) <= 0`)

Walls are detected using the **BLUE** tile value.

---

### Decision Logic
- **DOT-First Policy**  
  Pac-Man always prioritizes moving toward DOTs.

- **POWER Usage**  
  POWER is selected only when:
  - No DOTs are reachable
  - POWER is very close
  - A danger ghost is nearby and POWER provides a safer option

- **Escape Mode**  
  If a danger ghost is closer than `ESCAPE_TRIGGER`, Pac-Man switches to escape mode and prioritizes:
  - Increasing distance from ghosts
  - Moving through tiles with more exits (junctions)

---

### Move Scoring & Tie-Breaking
When multiple legal moves exist, each move is scored using:
- DOT distance reduction (main factor)
- Immediate reward (standing on DOT / POWER)
- Number of exits (trap avoidance)
- Light safety shaping (only when danger is near)

---

### Loop & Stuck Prevention
- Recent positions are stored in a small memory queue
- Returning to recent tiles is penalized
- If Pac-Man gets stuck, it forces DOT progress when safe

---

### Summary
This algorithm is **aggressive, score-oriented, and stable**:
- Eats DOTs quickly
- Escapes only when necessary
- Avoids corner camping and infinite loops


- - -

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
