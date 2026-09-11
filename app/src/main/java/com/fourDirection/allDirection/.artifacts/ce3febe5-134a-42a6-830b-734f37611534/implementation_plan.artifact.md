# Implementation Plan: Mapbox Routing System

Implement a multi-waypoint routing system within the [ExplorePage](file:///C:/Users/USER/AndroidStudioProjects/AllDirection/app/src/main/java/com/fourDirection/allDirection/page/main/ExplorePage.kt) using the Mapbox Directions API. Users will be able to plan routes with a start, end, and multiple intermediate waypoints, viewing the route overview and details (distance/duration) on the map.

## Proposed Changes

### [Data Layer]

#### [NEW] [RouteInfo.kt](file:///C:/Users/USER/AndroidStudioProjects/AllDirection/app/src/main/java/com/fourDirection/allDirection/data/RouteInfo.kt)
Define data models for the routing system.
- `Waypoint`: Represents a single point in the trip with a name and coordinates.
- `RouteInfo`: Contains the route geometry (list of points), total distance, and duration.

#### [NEW] [DirectionsRepository.kt](file:///C:/Users/USER/AndroidStudioProjects/AllDirection/app/src/main/java/com/fourDirection/allDirection/data/DirectionsRepository.kt)
Create a repository to interact with the Mapbox Directions API.
- Use `io.ktor.client.HttpClient` to make requests to `api.mapbox.com/directions/v5/mapbox/driving`.
- Implement `getRoute(waypoints: List<Point>)` to fetch and parse route data.
- Include helper functions to format distance (km) and duration (mins/hours).

---

### [Logic Layer]

#### [MODIFY] [ExploreViewModel.kt](file:///C:/Users/USER/AndroidStudioProjects/AllDirection/app/src/main/java/com/fourDirection/allDirection/page/main/ExploreViewModel.kt)
Extend the ViewModel to manage routing state and logic.
- **State**:
    - `isRoutingMode`: Boolean to toggle the routing UI.
    - `waypoints`: `MutableStateFlow<List<Waypoint>>` (initialized with two empty waypoints for Start and End).
    - `currentRoute`: `MutableStateFlow<RouteInfo?>`.
    - `isCalculatingRoute`: Boolean for loading states.
- **Methods**:
    - `toggleRoutingMode()`: Switch between normal search and routing.
    - `addWaypoint()`: Insert a new intermediate waypoint.
    - `removeWaypoint(index: Int)`: Remove a waypoint.
    - `updateWaypoint(index: Int, suggestion: SearchSuggestion)`: Set the location for a specific waypoint.
    - `calculateRoute()`: Trigger the repository to fetch the route based on current waypoints.
    - `clearRoute()`: Reset the routing state.

---

### [UI Layer]

#### [NEW] [RoutingPanel.kt](file:///C:/Users/USER/AndroidStudioProjects/AllDirection/app/src/main/java/com/fourDirection/allDirection/page/main/RoutingPanel.kt)
Create a new composable for the routing interface.
- A vertical list of `WaypointInput` fields.
- Buttons to add waypoints, swap start/end, and "Get Directions".
- Integration with the search suggestion logic to populate waypoint addresses.
- An "Overview" section showing distance and duration once a route is calculated.

#### [MODIFY] [ExplorePage.kt](file:///C:/Users/USER/AndroidStudioProjects/AllDirection/app/src/main/java/com/fourDirection/allDirection/page/main/ExplorePage.kt)
Integrate the routing UI and map visualization.
- Add a "Route" FAB to enter routing mode.
- Conditionally show `RoutingPanel` instead of the standard search bar.
- **Map Visualization**:
    - Use `PolylineAnnotationGroup` (or similar Mapbox v11 Compose component) to draw the `currentRoute.points` on the map.
    - Automatically adjust the map viewport to fit all waypoints and the route polyline.

## Verification Plan

### Automated Tests
- Unit tests for `DirectionsRepository` parsing (using mock JSON).
- ViewModel tests for waypoint management and route calculation triggers.

### Manual Verification
1.  Open **Explore** tab.
2.  Tap the **Plan Route** button.
3.  Enter a start address and an end address using search suggestions.
4.  Add an intermediate waypoint and search for a location.
5.  Tap **Get Directions**.
6.  Verify the blue polyline appears on the map connecting all points.
7.  Verify the distance and duration are displayed correctly.
8.  Exit routing mode and verify the map returns to normal search state.
