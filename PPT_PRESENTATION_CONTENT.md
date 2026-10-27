# B25CSI302 Java Mini Project - Presentation Content
## Slide-by-Slide Guide for B25CSI302_Java_Mini_Project_PPT_Template.pdf
### Student: ATHISH M (711525BEC014) | Project 35: Multi-Channel Data Acquisition Simulator

---

### Slide 1: Title Slide
- Project Title: Multi-Channel Data Acquisition Simulator
- Course: B25CSI302 – Java Programming (Regulation R2025)
- Year / Semester / Session: B.E. 2nd Year – 3rd Sem., Session 2026-27 (ODD)
- Name of the Student: ATHISH M
- Register Number: 711525BEC014
- Guide: Mr. K. Rajaram, Assistant Professor / CSE
- Department: Department of Electronics and Communication Engineering
- Institution: KIT – Kalaignarkarunanidhi Institute of Technology, Coimbatore

---

### Slide 2: Problem Statement and Objectives
- Problem Statement:
  In multi-sensor instrumentation systems, concurrent data streams running at asynchronous sampling rates cause race conditions, corrupted log files, and temporal disorder without strict thread synchronization and time-stamped buffering.
- Objectives:
  1. Concurrently simulate 4 sensor channels on dedicated worker threads at different sampling rates (2 Hz, 5 Hz, 10 Hz, 20 Hz).
  2. Implement a thread-safe, synchronized shared logger that interleaves multi-channel readings into a single time-ordered CSV file.
  3. Provide a live JavaFX dashboard with per-channel color coding, channel filtering, live frequency controls, and persistent counters.
  4. Prevent application crashes via custom exceptions (InvalidRateException, LoggerException) when invalid rates or file errors occur.

---

### Slide 3: Proposed Solution
- What the user does:
  Controls acquisition via Start/Stop/Clear buttons, modifies sampling rates on-the-fly, pauses/resumes individual channels, filters channels in the live table, and observes persistent stats.
- What the program does with it:
  Spawns independent channel threads that push readings to a synchronized PriorityQueue. A background drainer thread releases samples older than a 100 ms watermark window to disk and forwards batches to the JavaFX UI via Platform.runLater().
- What the user gets back:
  A flicker-free, real-time JavaFX table of interleaved data, continuous stats counters, and an append-mode CSV log (daq_log.csv) whose state survives app restarts (daq_state.properties).
- Why this is an object-oriented solution:
  Physical sensors are mapped to a polymorphic hierarchy (Channel abstract base class with TemperatureChannel, PressureChannel, VoltageChannel, and VibrationChannel), decoupled from the logging mechanism via the SampleSink interface.

---

### Slide 4: Module / Block Diagram
- Diagram Image: slide4_module_block_diagram.png (Insert full slide or center)
- Pipeline Architecture Overview:
  - Block 1 (Input Stage - Producers): 4 concurrent worker threads running periodic timing loops (Temperature @ 2 Hz, Pressure @ 5 Hz, Voltage @ 10 Hz, Vibration @ 20 Hz).
  - Connector 1: Invokes submit(Sample) through SampleSink interface across thread boundaries.
  - Block 2 (Processing Stage 1 - Monitor Buffer): SharedLogger critical section monitor lock (synchronized) feeding a PriorityQueue<Sample> sorted by timestamp.
  - Connector 2: Invokes flushReady(watermark) passing samples where elapsedMs <= (now - 100 ms).
  - Block 3 (Processing Stage 2 - Consumer Drainer): LogDrainer thread running every 50 ms enforcing the 100 ms sliding watermark reorder barrier.
  - Block 4 (Storage Block - Persistence): Appends interleaved CSV records to daq_log.csv and serializes session state to daq_state.properties.
  - Block 5 (Output / GUI Stage - Presentation): Event-driven JavaFX dashboard updating TableView<Sample> and metrics safely via Platform.runLater().

---

### Slide 5: Description of the Modules
- 1. Channel Producer Threads (Input Module)
  - What it holds: Channel metadata (name, unit, rateHz, running), wave parameters, and nano-second drift timer.
  - What it does: Concurrently generates sensor readings using drift-compensated timing loops and mathematical waveforms.
  - What it hands over: Immutable timestamped Sample objects dispatched via SampleSink.submit(sample).
- 2. SharedLogger Buffer (Processing Stage 1)
  - What it holds: Java monitor lock (synchronized), monotonic SessionClock, and min-heap PriorityQueue<Sample> sorted by timestamp.
  - What it does: Eliminates race conditions by guarding critical section writes and temporal interleaving across multi-rate channels.
  - What it hands over: Chronologically sorted candidate sample batches accessed via flushReady(watermark).
- 3. LogDrainer Worker (Processing Stage 2)
  - What it holds: Dedicated consumer thread reference, 50 ms polling loop, and 100 ms sliding watermark barrier threshold.
  - What it does: Filters out ready samples where elapsedMs <= (now - 100 ms), ensuring slower channels are never overtaken by faster ones.
  - What it hands over: Complete, time-ordered List<Sample> flushed simultaneously to disk storage and the UI thread.
- 4. File Storage Module (Persistence Stage)
  - What it holds: BufferedWriter append file handle for daq_log.csv and Properties dictionary for daq_state.properties.
  - What it does: Persistently writes CSV records to disk and serializes channel rates, pause states, and lifetime statistics.
  - What it hands over: Durable historical audit log files and state recovery properties loaded during application launch.
- 5. JavaFX User Interface (Output & Presentation Stage)
  - What it holds: TableView<Sample>, FilteredList, dynamic rate text inputs, pause checkboxes, and throughput counters.
  - What it does: Schedules thread-safe UI updates via Platform.runLater(), renders color-coded sensor rows, and binds user inputs.
  - What it hands over: Real-time visual feedback, status alerts, and user command events back to channel threads.

---

### Slide 6: Class Diagram
- Diagram Image: slide6_uml_class_diagram.png (High-Resolution 3840x2160 Widescreen 16:9 Diagram)
- Core Relationships:
  - Inheritance (extends):
    - Concrete sensors (TemperatureChannel, PressureChannel, VoltageChannel, VibrationChannel) extend abstract Channel.
    - Custom user exceptions (InvalidRateException, LoggerException) extend Java Exception.
  - Interface Implementation (implements):
    - Channel and LogDrainer implement java.lang.Runnable.
    - SharedLogger implements custom written SampleSink.
    - Sample implements java.lang.Comparable<Sample>.
  - Associations (has-a / owns):
    - MainApp owns AcquisitionManager, StateStore, and ChannelControl widgets.
    - ChannelControl binds Channel and delegates rate persistence to StateStore.
    - AcquisitionManager owns Channel collection, SharedLogger, and LogDrainer.
    - SharedLogger owns PriorityQueue<Sample> (min-heap buffer) and SessionClock.
    - LogDrainer flushes SharedLogger and forwards ordered batches to MainApp via Platform.runLater().

---

### Slide 7: Classes and Their Responsibilities

**1. Application Entry Point & GUI Stage**
- **MainApp** *(Holds main(), extends Application)*: The primary application coordinator; initializes the JavaFX GUI stage, sets up TableView data binding, manages Start/Stop lifecycle events, and executes the 250 ms stats refresh timeline.
- **ChannelControl** *(UI Component Widget, extends HBox)*: Manages user interaction controls for an individual sensor channel, binding its sampling rate text box, pause/resume checkbox, and status message callbacks.

**2. Simulation & Channels Stage (Inheritance Hierarchy)**
- **Channel** *(Abstract Base Class, implements Runnable)*: Regulates channel thread execution, drift-compensated nano-timer pacing loops, frequency rate bounds checking, and thread-safe sample count tallying.
- **TemperatureChannel** *(Subclass, extends Channel)*: Simulates slow thermal ambient temperature at 2 Hz (baseline 25.0 °C) with ambient sinusoidal drift.
- **PressureChannel** *(Subclass, extends Channel)*: Simulates atmospheric barometric pressure at 5 Hz (baseline 101.3 kPa) with atmospheric fluctuations.
- **VoltageChannel** *(Subclass, extends Channel)*: Simulates analog DC rail voltage at 10 Hz (baseline 3.30 V) with high-frequency 50 Hz AC sinusoidal ripple.
- **VibrationChannel** *(Subclass, extends Channel)*: Simulates mechanical vibration at 20 Hz (baseline 0.0 g) with pseudo-random transient shock impulses.

**3. Processing & Thread-Safe Logging Stage**
- **AcquisitionManager** *(System Controller)*: Orchestrates the concurrency subsystem; instantiates, starts, monitors, and cleanly terminates the 4 sensor threads, the shared logger, and the background drainer.
- **SharedLogger** *(Synchronized Monitor, implements SampleSink)*: Synchronizes sample submissions across asynchronous threads, buffers readings into a min-heap PriorityQueue, and appends time-ordered records to daq_log.csv.
- **LogDrainer** *(Consumer Worker Thread, implements Runnable)*: Periodically polls SharedLogger every 50 ms to flush ready samples older than the 100 ms watermark barrier and safely dispatches batches to the JavaFX thread via Platform.runLater().
- **SessionClock** *(Monotonic Time Service)*: High-resolution monotonic clock providing drift-free elapsed milliseconds from nanoTime() and formatting synchronized ISO wall-clock timestamps.
- **Sample** *(Immutable Model Record, implements Comparable<Sample>)*: Encapsulates an individual sensor measurement (index, timestamp, channel, value, unit) and defines natural chronological ordering for PriorityQueue sorting.

**4. Storage & Persistence Module**
- **StateStore** *(Persistence Service)*: Manages session configuration persistence using java.util.Properties, saving and reloading channel frequencies, pause states, session counts, and lifetime sample totals.

**5. Custom Written Interfaces & Standard Interfaces**
- **SampleSink** *(Custom Written Interface)*: Decouples sensor channel threads from the logger implementation by providing a unified submit(Sample s) contract.
- **Runnable** *(Java Standard Interface)*: Contract implemented by Channel and LogDrainer allowing them to execute concurrently on separate Java threads.
- **Comparable<Sample>** *(Java Standard Interface)*: Contract implemented by Sample to enable min-heap temporal sorting in PriorityQueue.

**6. User-Defined Exceptions**
- **InvalidRateException** *(User-Defined Checked Exception, extends Exception)*: Thrown when a user or channel attempts to set a frequency outside the safe [1..100 Hz] operating band.
- **LoggerException** *(User-Defined Checked Exception, extends Exception)*: Thrown when file output encounters I/O errors or when write operations are attempted on a closed logger.

---

### Slide 10: Output — Screenshots
- **Normal Case (`slide10_screenshot1_normal_case.png`)**: Real-time JavaFX dashboard running all 4 channels at default rates (2 Hz, 5 Hz, 10 Hz, 20 Hz) with live color-coded TableView, incrementing sample counters, and green active status bar.
- **Custom Inputs & Filtering (`slide10_screenshot2_custom_inputs.png`)**: Demonstrates dynamic non-hardcoded operation with Vibration increased to 40 Hz, Temperature set to 10 Hz, Pressure paused, and the live table filtered exclusively to Vibration.
- **Audit File Output (`slide10_screenshot3_csv_output.png`)**: Generated `daq_log.csv` opened in editor displaying the CSV header, session start marker, and strictly time-ordered interleaved records across all channels.
- **Exception Handling Bonus (`slide11_screenshot_exception_handling.png`)**: Invalid input `500` entered in the rate box, caught by `InvalidRateException`, and safely displaying a red warning banner without crashing.

---

### Slide 11: Testing and Exception Handling
- Wrong Input 1: Entering 500 in the Vibration rate box.
  - Program prints: "Rate 500 Hz out of bounds [1..100]" in red status bar; rate reverts safely.
- Wrong Input 2: Entering non-numeric text like abc.
  - Caught as NumberFormatException; displays "Rate must be a whole number".
- User-Defined Exception: InvalidRateException protects system timing stability from thread starvation or scheduling collapse.

---

### Slide 12: Innovation or Uniqueness
1. Dynamic Real-Time Rate Reconfiguration: Sampling frequencies can be changed during active acquisition; internal timing schedules recalculate drift on the fly.
2. Crash-Resilient State & Tail Recovery: On startup, the previous session settings and the last 200 logged samples are automatically restored into the table from daq_state.properties and daq_log.csv.

---

### Slide 13: Software & Hardware Requirements
- JDK: Oracle OpenJDK 21
- JavaFX SDK: JavaFX 21 (controls, graphics, base)
- IDE: Eclipse / VS Code / IntelliJ IDEA
- Diagram Tool: Draw.io / Mermaid UML
- Hardware: Standard laptop or laboratory PC (Windows / Linux / macOS)

---

### Slide 14: Applications
- Electronics & Instrumentation Lab: Multi-channel logging from digital oscilloscopes, power rails, and multimeters into a standardized laboratory dataset.
- Department & College Testing Facilities: Automated monitoring of environmental test chambers (temperature and vibration stress rigs).
- Industrial Telemetry & SCADA Systems: Edge sensor node aggregator buffering asynchronous sensor streams before transmission over Industrial IoT gateways.

---

### Slide 15: Challenges Faced & Solutions
1. Problem: Fast channels (20 Hz) produced readings before slow channels (2 Hz), causing out-of-order writes in the CSV log.
   Solution: Introduced a PriorityQueue buffer and a 100 ms watermark sliding window (reorderWindowMs).
2. Problem: Updating JavaFX TableView directly from background threads triggered IllegalStateException (Not on FX application thread).
   Solution: Utilized Platform.runLater(() -> appendRows(batch)) inside LogDrainer to batch UI updates safely.
3. Problem: Live rate modifications caused drift and burst sampling.
   Solution: Implemented drift resetting in Channel.run() whenever rateHz != lastRate.

---

### Slide 16: Learning Outcomes
1. Mastered multi-threaded programming in Java with disciplined mutex synchronization (synchronized) and lock-free primitives (AtomicLong, volatile).
2. Designed and implemented a robust Producer-Consumer architecture using Java Collections (PriorityQueue, LinkedHashMap).
3. Gained hands-on experience in building event-driven JavaFX GUIs with thread-safe UI updates and state persistence.

---

### Slide 17: References
- Schildt, H. (2024). Java: The Complete Reference, 13th Edition, McGraw-Hill.
- Horstmann, C. S. (2024). Core Java Volume I – Fundamentals, 12th Edition, Pearson.
- Oracle Java Documentation: docs.oracle.com/en/java/javase/21/
