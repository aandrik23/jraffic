# Jraffic

Jraffic is a dependency-free Java Swing simulation of an adaptive four-way traffic junction. Each road has one incoming and one outgoing lane. Vehicles choose an immutable left, straight, or right route when spawned.

## Requirements

- JDK 25
- A graphical desktop environment

No Maven, Gradle, JavaFX, or third-party dependencies are required.

## Build and Run

From the repository root:

```sh
mkdir -p out
javac --release 25 -d out src/jraffic/*.java
java -cp out jraffic.Main
```

The window scales to the available size while keeping the simulation geometry proportional.

## Controls

| Key | Vehicle origin and movement |
| --- | --- |
| Up arrow | From South, moving north toward the junction |
| Down arrow | From North, moving south toward the junction |
| Right arrow | From West, moving east toward the junction |
| Left arrow | From East, moving west toward the junction |
| `R` | Random available direction and random route |
| Escape | Exit the simulation |

Every spawn gets a random route. A key press is rejected if the selected lane is full or if its spawn point does not have a safe gap. Holding or repeatedly pressing a key therefore cannot stack vehicles on top of each other.

## Route Colors

| Color | Route |
| --- | --- |
| Pink | Left turn |
| Blue | Straight |
| Yellow | Right turn |

The route is selected once at creation and never changes.

## Traffic Strategy

There is one red/green light at every incoming lane. Only one approach is green at a time, so left, straight, and right movements from different approaches cannot conflict. A signal change has an all-red clearance period of at least one second. The controller will not grant the next green while a vehicle remains inside the intersection.

The controller scores waiting approaches using:

- Current queue length
- Time since the approach was last served
- A high-priority bonus when the queue reaches lane capacity

A green lasts at least 3 seconds. Its target duration grows from 5 to 12 seconds according to the current queue's capacity ratio. The 12-second limit prevents one busy approach from starving every other road. If an approach empties, its green can finish early after the minimum duration.

## Congestion Capacity

Simulation dimensions are measured in logical pixels:

```text
lane length    = 330
vehicle length = 28
safety gap     = 16

capacity = floor(330 / (28 + 16)) = 7 vehicles
```

The dashboard displays each live queue as `vehicles / capacity`. A full queue receives the controller's highest service priority and the maximum green extension. New vehicles are rejected at capacity, so queues cannot overflow their physical lanes.

## Vehicle Safety

- Moving vehicles use a fixed speed of 82 simulation units per second.
- Vehicles stop exactly before the stop line while their approach is red.
- A follower's movement is limited so it remains at least 16 units behind the preceding vehicle after accounting for vehicle length.
- Vehicles already committed to the intersection finish their route during an all-red transition.
- New vehicles require one vehicle length plus the safety gap at the spawn point.

## Tests

The test suite uses no external framework:

```sh
mkdir -p out
javac --release 25 -d out src/jraffic/*.java test/jraffic/*.java
java -cp out jraffic.SimulationTests
```

It checks capacity calculation, spawn throttling, red-light stopping, following distance, congestion-based green extension, intersection clearance, and waiting-lane service.
