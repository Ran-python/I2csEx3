# Pac-Man Project – Custom Server & Algorithm Integration

## Overview
This project is a **full standalone implementation of the Pac-Man game server**, built entirely from scratch without relying on the lecturer’s provided executable (`exe.ex3`).

Course: Introduction to Computer Science Institution: Ariel University, School of Computer Science Year: 2026

The system preserves **full compatibility with the lecturer’s algorithm interface**, while replacing:
- The game engine
- Board handling
- Ghost logic
- Power mechanics
- GUI rendering

The result is a **clean, extensible, server-authoritative Pac-Man implementation** that can run the lecturer’s algorithm unchanged.

---

##  Part 1 – Algorithm Integration (`Ex3Algo`)

###  Goal
The goal was to execute the lecturer’s provided algorithm (`Ex3Algo`) **without modifying it**, while running on a **completely custom game engine**.

### Core Design Principle
Instead of adapting the algorithm, the project **reimplements the `PacManGame` interface** exactly as expected by the course API.

This ensures:
- Full compatibility
- Zero changes to the algorithm
- Correct behavior according to course rules

###  How the Integration Works
- `MyPacmanGame` implements `PacManGame`
- On every game tick:
  1. The engine calls `algo.move(this)`
  2. The algorithm returns a direction (`UP / DOWN / LEFT / RIGHT / STAY`)
  3. The engine applies the move internally
- The algorithm never mutates game state directly

###  Compatibility Guarantees
The following expectations of the lecturer’s algorithm are preserved:
- `getGame()` returns `int[][]`
- `getPos()` returns `"x,y"`
- `getGhosts()` returns `GhostCL[]`
- Direction constants are identical
- `UP` increases Y (critical for correctness)

**Result:**  
Any valid Ex3 algorithm can run on this server **unchanged**.

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
✔ Cyclic maps, power pellets, ghost AI  
✔ Server-authoritative architecture  

This project demonstrates:
- Interface-driven design
- Game engine architecture
- Clean separation between server, algorithm, and GUI

---
