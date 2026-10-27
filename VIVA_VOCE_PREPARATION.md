# B25CSI302 Java Mini Project - Viva Voce & Demonstration Guide
## Candidate: ATHISH M (711525BEC014) · Project 35: Multi-Channel Data Acquisition Simulator
### Evaluator: Mr. Rajaram K, Assistant Professor / CSE

---

## 1. Top Viva Questions & Precise Technical Answers

### Q1: Why is `synchronized` used on `SharedLogger`? What would break if it were removed?
**Answer:**  
"Four concurrent channel threads call `submit()` simultaneously, while the `LogDrainer` thread calls `flushReady()`. Both methods read and modify the shared `PriorityQueue<Sample>`, the `BufferedWriter`, and sample index counters. If `synchronized` is removed:
1. `PriorityQueue` is **not thread-safe**; concurrent insertions and polls cause `ConcurrentModificationException`, internal heap array corruption, or null pointer dereferences.
2. The file writer `out.write()` would interleave raw character bytes from multiple threads, producing corrupted, unparseable CSV rows.
3. The sample index `nextIndex++` would suffer from lost updates (a classic read-modify-write race)."

---

### Q2: Why is there a 100 ms reorder window (`reorderWindowMs`)?
**Answer:**  
"Because channels sample at different frequencies (2 Hz vs. 20 Hz), the vibration channel samples every 50 ms while the temperature channel samples every 500 ms. If we flush immediately:
Suppose Temperature samples at $t = 10\text{ ms}$, but before its thread acquires the lock, Vibration generates samples at $t = 20\text{ ms}$ and $t = 70\text{ ms}$ and submits first.
By holding samples in a `PriorityQueue` and releasing only those where `sample.elapsedMs <= currentClock - 100 ms`, we give slower threads time to deliver their earlier readings into the heap. When popped, samples are strictly in chronological order."

---

### Q3: Why are `rateHz`, `enabled`, and `running` declared as `volatile`?
**Answer:**  
"In Java, each thread can cache field values in CPU registers or L1/L2 caches. When the user changes a rate or unticks a checkbox in the JavaFX UI, the **JavaFX Application Thread** modifies these fields. Because the worker threads run on separate CPU cores, without `volatile` they might never observe the updated value due to memory caching. `volatile` enforces a **happens-before relationship** and guarantees immediate cross-thread visibility without locking."

---

### Q4: Why can't we update the JavaFX TableView directly inside `LogDrainer`?
**Answer:**  
"JavaFX enforces a single-threaded GUI model. Only the **JavaFX Application Thread** is permitted to mutate scene graph nodes or bound collections (`ObservableList`). If `LogDrainer` updated `rows.addAll(batch)` directly, JavaFX would throw `java.lang.IllegalStateException: Not on FX application thread`. To solve this, `LogDrainer` passes the batch to `Platform.runLater(() -> appendRows(batch))`."

---

### Q5: How does the application prevent crashing on invalid user input?
**Answer:**  
"In `Channel.java`, `validateRate(int hz)` checks if the entered rate is between 1 and 100 Hz. If outside this range, it throws our custom checked exception `InvalidRateException`. In `ChannelControl.java`, the event handler catches `NumberFormatException` (for non-integer input) and `InvalidRateException`, reverts the text field back to the current valid rate, and displays a red error status in the GUI status bar, keeping the simulator running stably."

---

### Q6: How does application state survive a restart?
**Answer:**  
"When the user clicks **Stop** or closes the window (`stage.setOnCloseRequest`), `StateStore.java` writes the active rate and enabled status of each channel, along with session count and lifetime logged samples, into `daq_state.properties` using `Properties.store()`. On the subsequent launch, `StateStore.load()` restores all channel configurations, and `SharedLogger.readTail()` reads the last 200 rows from `daq_log.csv` directly into the table."

---

## 2. Live Code Modification Challenges (Examiner Test)

The project rubric states:  
*"Ask each of them to change one thing in front of you and rerun it. A student who wrote the program can do that in a minute; a student who did not cannot do it at all."*

Here are the 3 most common modifications the examiner may ask:

### Challenge A: "Change the default rate of the Temperature channel from 2 Hz to 4 Hz."
- **File to open:** `src/main/java/com/athish/daq/channels/TemperatureChannel.java`
- **Line to modify:**
  ```java
  public TemperatureChannel() {
      super("Temperature", "C", "#ff8a80", 2); // Change 2 to 4
  }
  ```
- **Action:** Change `2` to `4`, save, and run `run.bat` (or recompile with `javac`).

---

### Challenge B: "Change the maximum allowed rate from 100 Hz to 200 Hz."
- **File to open:** `src/main/java/com/athish/daq/core/Channel.java`
- **Line to modify:**
  ```java
  public static final int MAX_RATE_HZ = 100; // Change 100 to 200
  ```
- **Action:** Change `100` to `200`, save, and re-run. Now entering `150` in the UI is accepted!

---

### Challenge C: "Add a 5th channel (e.g., Humidity Channel at 1 Hz, Unit '%', Color '#80cbc4')."
1. In `src/main/java/com/athish/daq/channels/`: Create `HumidityChannel.java`:
   ```java
   package com.athish.daq.channels;
   import com.athish.daq.core.Channel;
   public class HumidityChannel extends Channel {
       public HumidityChannel() {
           super("Humidity", "%", "#80cbc4", 1);
       }
       @Override
       protected double nextValue(double tSeconds) {
           return 55.0 + 10.0 * Math.sin(0.05 * tSeconds);
       }
   }
   ```
2. In `src/main/java/com/athish/daq/core/AcquisitionManager.java`:
   Add `new HumidityChannel()` into `List.of(...)` on line 33:
   ```java
   List<Channel> all = List.of(
       new TemperatureChannel(), new PressureChannel(),
       new VoltageChannel(), new VibrationChannel(),
       new HumidityChannel()
   );
   ```
   *Everything else (threads, UI control row, filter box, file writing) automatically adapts because of polymorphism!*

---

## 3. How to Run During Demonstration

1. Open PowerShell or Command Prompt in the project folder:
   ```cmd
   run.bat
   ```
   *(Auto-detects JavaFX at `C:\javafx-sdk-21.0.13\lib`)*

2. To run headless unit tests (no GUI required):
   ```cmd
   java -cp out com.athish.daq.DaqSelfCheck
   ```
   *(Executes in 4 seconds, prints PASS for all 8 validation checks!)*
