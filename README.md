# CHORD P2P protocol

This is an implementation of the CHORD P2P protocol, as it is discussed in the original paper.
The project part of the Distributed Systems course of Politecnico di Milano, a.y. 2018-2019.

## Getting Started

The protocol is implemented as a Java library and tested on a toy application which benefits from all the features provided by CHORD.

### Launch the application

Digit the following command to execute the program:

```
java -jar Chord_app.jar
```

### Create or join a ring

After the program the application launch, the following menu will appear:

```
Digit an option:
1. CREATE a new ring
2. JOIN an existing ring
```

Digit the relevant number of the action you want to choose, than press `ENTER`.
If you digit `2`, a new line will appear asking for the IP address to search for:

```
Insert the IP to join:
```

### Node options

Once you joined the ring, the following new menu will show you all the available options:

```
Digit an option:
1. PRINT your node information
2. PRINT your finger table
3. LOOKUP for a key in the ring
4. LEAVE the ring
```

## Requirements
* [Java Runtime Environment](https://www.java.com/it/download/) - Necessary to execute the application.

## Original work
This project is based on the original CHORD paper:
* ["Chord: A scalable peer-to-peer lookup service for internet applications", August 2001] (https://dl.acm.org/doi/pdf/10.1145/383059.383071)
