# CS360
Course work for CS360

# Mobile App Portfolio: Weight Tracker

### App Requirements, Goals, and User Needs
The core requirement of this project was to develop a fully functional Android mobile application that implements user authentication, persistent data storage via CRUD operations, and hardware-level OS integrations. The goal was to build a lightweight, local-first weight tracking utility. This application was designed to address a specific user need: providing a reliable, privacy-focused fitness tool that allows users to log their data without being forced into mandatory cloud accounts, subscriptions, or constant internet connectivity.

### User-Centered UI and Screen Design
To support these needs, the app utilizes a streamlined set of screens, primarily an authentication view and a central dashboard. My design approach prioritized reducing friction and eye strain. I implemented a strict dark-mode-first aesthetic using the Catppuccin Mocha color palette, accompanied by clean UI elements like custom wave backgrounds and trend pills. This design is successful because it surfaces the core CRUD operations directly on the main dashboard, allowing the user to open the app, log their weight, and immediately close it without digging through nested menus.

### Coding Approach and Strategies
My development strategy is heavily iterative and backend-first. Before touching the frontend layout, I built the SQLite database helpers to ensure user sessions and data inputs were actually persisting locally on the device. Once the backend architecture was stable, I wired up the frontend UI elements one by one. Moving forward, I plan to continue utilizing this local-first SQLite strategy, as establishing a rock-solid data layer first prevents massive debugging headaches when linking dynamic UI threads later on.

### Testing and Functional Verification
I tested the code iteratively, verifying each feature as it was built rather than waiting for a massive final compilation. This involved testing edge cases like null database inputs, rapid CRUD operations, and UI scaling across different screen sizes. Testing revealed a significant hardware-level quirk: the Android emulator would silently drop loopback messages triggered by the `SmsManager`. This made it critical to test the `.apk` directly on a physical device via ADB to verify that the target weight condition was properly triggering the native OS permission.

### Overcoming Challenges through Innovation
The biggest challenge throughout the development lifecycle was mapping backend data operations to a dynamic frontend GUI without crashing the main UI thread. Wiring up native hardware permissions (like the SMS manager) to fire exactly when a specific database milestone was reached required careful attention to application state and asynchronous execution. 

### Core Successes and Demonstrated Skills
I was particularly successful in demonstrating my backend integration skills. Building the local SQLite architecture from scratch—managing tables, handling version upgrades, and executing precise CRUD operations—resulted in a highly responsive app. Connecting that stable data layer to custom Material themes and hardware-level triggers proved my ability to bridge the gap between strict backend logic and a seamless user experience.
