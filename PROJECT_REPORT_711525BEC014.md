# B25CSI302 – Java Programming Mini Project Report
## Regulation R2025 · Academic Year 2026–2027 (ODD)
### Department of Electronics and Communication Engineering
### KIT – Kalaignarkarunanidhi Institute of Technology, Coimbatore

---

**Project Title:** Multi-Channel Data Acquisition Simulator  
**Project No. / Level:** Project 35 · Level 4 (Advanced)  
**Student Name:** ATHISH M  
**Register Number:** 711525BEC014  
**Class / Semester:** II Year B.E. ECE – Section A / Semester III  
**Course Faculty:** Mr. Rajaram K, Assistant Professor / CSE  

---

## 1. Problem Statement and Objectives

### 1.1 Problem Statement
In industrial instrumentation and electronic embedded systems, sensors (such as temperature probes, pressure transducers, voltage monitors, and vibration accelerometers) generate readings at vastly different sample rates. Without thread synchronization and disciplined buffering, logging readings from concurrent acquisition channels results in data race conditions, file corruption, missed samples, or out-of-order temporal sequences that invalidate subsequent signal analysis.

### 1.2 Objectives
1. **Multi-Threaded Acquisition:** Model each sensor channel as an independent worker thread producing samples at designated frequencies (Temperature at 2 Hz, Pressure at 5 Hz, Voltage at 10 Hz, Vibration at 20 Hz).
2. **Synchronized Shared Logger:** Eliminate race conditions by routing all channel outputs into a thread-safe, synchronized buffer.
3. **Time-Ordered Interleaving:** Guarantee strict temporal ordering of samples in the output log using a sliding watermark window (100 ms) and a `PriorityQueue`, preventing high-speed channels from overtaking earlier samples produced by slower channels.
4. **Live JavaFX Dashboard:** Display live readings in an event-driven JavaFX table with per-channel color highlights, channel filtering, live rate adjustment, and thread performance counters.
5. **Durable File I/O & State Persistence:** Append session readings into a persistent CSV file (`daq_log.csv`) and save application state (`daq_state.properties`) so user configurations and lifetime sample statistics survive across application restarts.
6. **Robust Exception Handling:** Enforce bounds validation (1–100 Hz) using a custom `InvalidRateException`, and protect file transactions with a custom `LoggerException`, ensuring the application never crashes on invalid user input.

---

## 2. Proposed Solution & Architecture

### 2.1 Solution Overview
The application follows a decoupled Producer-Consumer architecture:
- **Producers (Channels):** Four distinct channel threads continuously evaluate physical/mathematical wave functions at their assigned sampling rate and submit `Sample` objects into the shared sink.
- **Buffer & Synchronization (SharedLogger):** Acts as the synchronization boundary. Channel threads invoke `submit(Sample)`. Internal structures are protected via `synchronized` monitor locks.
- **Consumer / Drainer (LogDrainer):** A dedicated background worker thread periodically wakes up (every 50 ms), extracts all samples whose timestamps are older than the 100 ms reorder window via `flushReady(false)`, writes them sequentially to `daq_log.csv`, and forwards the batch to the UI.
- **Presentation (MainApp):** JavaFX GUI thread receives sample batches via `Platform.runLater()`, updating the `TableView` and counters smoothly without blocking UI interactions.

### 2.2 Why This is an Object-Oriented Solution
Real-world physical instruments and logging mechanisms are mapped to discrete OOP abstractions:
- **`Sample`**: Immutable data carrier implementing `Comparable<Sample>` for deterministic chronological sorting.
- **`Channel`**: Abstract base class encapsulating thread control loops, timing schedules, rate validation, and atomic sample counting (`AtomicLong`).
- **`TemperatureChannel`, `PressureChannel`, `VoltageChannel`, `VibrationChannel`**: Subclasses demonstrating **inheritance** and **polymorphism** by overriding `nextValue(double tSeconds)` to simulate distinct physical sensors.
- **`SampleSink`**: Interface abstracting the sample destination, decoupling the channels from file writing specifics.
- **`SharedLogger`**: Encapsulates concurrency control, file streaming, and priority queue management.
- **`StateStore`**: Encapsulates persistence using the `java.util.Properties` API.

---

## 3. Module / Block Diagram

```
+-----------------------------------------------------------------------------------------+
|                                    INPUT CHANNELS (PRODUCERS)                           |
|                                                                                         |
|  +--------------------+   +--------------------+   +--------------------+   +---------+ |
|  | TemperatureChannel |   |  PressureChannel   |   |   VoltageChannel   |   |Vibration| |
|  |     (Thread 1)     |   |     (Thread 2)     |   |     (Thread 3)     |   | (Th. 4) | |
|  |       2 Hz         |   |       5 Hz         |   |       10 Hz        |   |  20 Hz  | |
|  +---------+----------+   +---------+----------+   +---------+----------+   +----+----+ |
+------------|------------------------|------------------------|-------------------|------+
             |                        |                        |                   |
             +------------------------+-----------+------------+-------------------+
                                                  |
                                                  v
                                      [submit(Sample)] (Synchronized)
+-----------------------------------------------------------------------------------------+
|                                    SHARED LOGGER (BUFFER)                               |
|                                                                                         |
|                        +----------------------------------------+                       |
|                        |  PriorityQueue<Sample> (Sorted by ms)  |                       |
|                        +----------------------------------------+                       |
|                                                  |                                      |
|                                        flushReady(watermark)                            |
+--------------------------------------------------|--------------------------------------+
                                                   v
+-----------------------------------------------------------------------------------------+
|                                  LOG DRAINER (CONSUMER THREAD)                          |
|                                        (Runs every 50 ms)                               |
+--------------------------------------------------|--------------------------------------+
                                                   |
                         +-------------------------+-------------------------+
                         |                                                   |
                         v                                                   v
+----------------------------------+               +-------------------------------------+
|        FILE SYSTEM STORAGE       |               |          JAVAFX USER INTERFACE      |
|  - daq_log.csv (Appended log)    |               |  - TableView (Live samples)         |
|  - daq_state.properties (State)  |               |  - Channel controls (Rate/Pause)    |
+----------------------------------+               |  - FilteredList & Live Stats        |
                                                   +-------------------------------------+
```

---

## 4. Class Responsibilities

1. `Sample`: Immutable record of a channel reading. Natural ordering based on `elapsedMs`.
2. `Channel` (abstract): Implements `Runnable`. Handles nano-second drift compensation, rate validation, pause/resume flags, and produces samples.
3. `TemperatureChannel`, `PressureChannel`, `VoltageChannel`, `VibrationChannel`: Concrete implementations overriding `nextValue(tSeconds)`.
4. `SampleSink` (interface): Interface exposing `void submit(Sample sample) throws LoggerException`.
5. `SharedLogger`: Synchronized thread-safe collector using `PriorityQueue<Sample>`. Writes to `daq_log.csv`.
6. `LogDrainer`: Periodic consumer thread that flushes samples older than the 100 ms reorder watermark.
7. `SessionClock`: High-resolution monotonic clock providing unified `elapsedMs` and wall timestamps.
8. `StateStore`: Reads and writes properties to `daq_state.properties`.
9. `InvalidRateException`: Custom checked exception thrown when sampling rate is outside $[1, 100]\text{ Hz}$.
10. `LoggerException`: Custom checked exception wrapping I/O and session lifecycle errors.
11. `MainApp`: JavaFX entry point; manages UI layout, table binding, event handlers, and lifecycle hooks.
12. `ChannelControl`: UI component (`HBox`) binding individual channel rate modification and pause/resume checkboxes.

---

## 5. Java Concepts Demonstrated

| Concept | Implementation in Project |
|---|---|
| **Classes & Objects** | Encapsulated in 16 structured classes across 4 packages. |
| **Inheritance & Overriding** | `TemperatureChannel`, `PressureChannel`, etc., extend `Channel` and override `nextValue(double tSeconds)`. |
| **Interfaces** | `SampleSink` implemented by `SharedLogger`; `Runnable` implemented by `Channel` and `LogDrainer`; `Comparable<Sample>` implemented by `Sample`. |
| **User-Defined Exceptions** | `InvalidRateException` (rates $<1$ or $>100\text{ Hz}$); `LoggerException` (file I/O or closed logger errors). |
| **Collections Framework** | `PriorityQueue` (temporal ordering buffer), `LinkedHashMap` (channel registry), `ArrayDeque` / `ArrayList` (log tail reading), `ObservableList` / `FilteredList` (UI table binding), `Properties` (key-value persistence). |
| **Multithreading** | 6 threads running concurrently (4 channel threads + 1 drainer thread + 1 JavaFX thread). |
| **Synchronization** | `synchronized` methods on `SharedLogger` (`submit`, `flushReady`, `close`) to prevent concurrent queue corruption and file interleaving races. |
| **Memory Visibility** | `volatile` modifier used on `rateHz`, `enabled`, and `running` flags across UI and worker threads. |
| **File I/O** | `BufferedWriter` with append mode for CSV streaming; `BufferedReader` for restoring session history. |
| **State Persistence** | `Properties` store/load mechanism saving channel rates, pause states, session count, and lifetime sample totals. |
| **JavaFX & Event Handling** | `TableView`, `FilteredList`, `Timeline`, `Button` and `CheckBox` actions, `Platform.runLater()` UI thread updates. |

---

## 6. Key Code Segment: Synchronized Watermark Reordering

```java
// SharedLogger.java: Ensures multi-rate samples are written strictly in time order
public synchronized List<Sample> flushReady(boolean all) throws LoggerException {
    if (!open) {
        throw new LoggerException("Logger is closed");
    }
    double watermark = clock.elapsedMs() - reorderWindowMs; // 100 ms sliding window
    List<Sample> written = new ArrayList<>();
    try {
        while (!pending.isEmpty() && (all || pending.peek().getElapsedMs() <= watermark)) {
            Sample s = pending.poll(); // PriorityQueue extracts lowest elapsedMs first
            s = s.stamped(nextIndex++, clock.wallTime(s.getElapsedMs()));
            out.write(s.toCsv());
            out.newLine();
            written.add(s);
        }
        out.flush();
    } catch (IOException e) {
        throw new LoggerException("Write to log file failed", e);
    }
    return written;
}
```
**Explanation:**  
Even though channels run at 2 Hz, 5 Hz, 10 Hz, and 20 Hz, thread scheduling jitter means a faster thread might submit a reading taken at $t = 120\text{ ms}$ before the 2 Hz thread submits its reading from $t = 100\text{ ms}$. By holding samples in a `PriorityQueue` until the session clock exceeds the reading's timestamp by 100 ms (`reorderWindowMs`), the logger guarantees no earlier sample can arrive later, ensuring 100% time-ordered file output without race conditions.

---

## 7. Testing and Exception Handling

### 7.1 Testing Rate Bounds (`InvalidRateException`)
- **Action:** User types `500` in the Vibration rate field and clicks **Apply**.
- **Result:** `Channel.validateRate(500)` throws `InvalidRateException("Rate 500 Hz out of bounds [1..100]")`. Caught in `ChannelControl.java`:
  ```java
  catch (InvalidRateException ex) {
      rateField.setText(String.valueOf(channel.getRateHz()));
      error.accept(ex.getMessage());
  }
  ```
  The UI displays a clear red status notification without terminating the thread or crashing the window.

### 7.2 Headless Test Suite (`DaqSelfCheck.java`)
A standalone test suite verifies all system invariants without requiring a display:
1. Rejection of invalid rates ($500\text{ Hz}$).
2. Strict monotonic progression of `elapsedMs` in `daq_log.csv`.
3. 1:1 parity between samples produced, logged, and rendered.
4. Channel sample frequency verification within statistical tolerances.

---

## 8. Real-World Applications & Innovation

1. **Electronics Testing Laboratory:** Simulating multi-point data acquisition from bench instruments (power supplies, function generators, temperature sensors) logging into unified test reports.
2. **Automotive / Aerospace Telemetry:** Ingesting asynchronous signals from engine sensors, vibration gyroscopes, and battery packs where strict time-stamping is essential for post-flight telemetry analysis.
3. **Innovation Beyond Requirements:**
   - **Live Dynamic Frequency Reconfiguration:** Rates can be altered on-the-fly while threads are actively running; timing drifts are dynamically recalculated.
   - **Automatic Log Recovery:** Upon application startup, previous logs are inspected, and the trailing 200 samples are restored into the JavaFX table, allowing seamless resumption of inspection sessions.

---

## 9. Learning Outcomes
1. **Thread Synchronization:** Understood how `synchronized` monitors protect shared collections (`PriorityQueue`) and file streams against concurrency races.
2. **Producer-Consumer Coordination:** Mastered decoupling production rates from I/O drain schedules using sliding-window watermarks.
3. **GUI Thread Safety:** Applied `Platform.runLater()` to safely bridge background worker threads with JavaFX UI controls.
4. **State Persistence in Java:** Utilized Java `Properties` and NIO paths to ensure complete configuration survival across restarts.

---

## 10. References
- Herbert Schildt, *Java: The Complete Reference*, 13th Edition, McGraw-Hill, 2024.
- Cay S. Horstmann, *Core Java Volume I – Fundamentals*, 12th Edition, Pearson, 2024.
- Oracle Java SE 21 Documentation: Concurrency & JavaFX Controls (`docs.oracle.com/en/java/javase/21/`).
