# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.5.0] - 2026-09-11

### Added
- Every exercise answers one list of actions: goals, swap, tie into a superset, move, remove
- A running session can now do what the plan editor could: swap an exercise, drop it, reorder it,
  tie a superset on the spot and set a goal for today without touching the program
- Order is changed by dragging, in the day editor and in the session alike
- Any exercise folds away, with the state kept while the session goes on
- Profile shows how long the training has been going, the tonnage, the streak and the last record
- Library sends an exercise straight into a day of the plan or into today's session
- Plans and gyms can be renamed, gyms can be copied with their equipment

### Changed
- Jura and Martian Mono replace Darker Grotesque and Space Mono: the old pair had no Cyrillic at
  all, so every Russian word was drawn by the system font
- Settings are laid out in sections of blocks, with an icon chip on every row
- Days of a plan stand in a row of pills instead of two arrows
- The set sheet puts its main button last, and reps are as large as the weight

### Fixed
- A superset tied during a session could not be performed: closing a round wrote nothing
- A set inside a superset round could not be written with its own weight, or corrected at all
- The gym name was lost when leaving the equipment screen by the system back gesture
- The library button had its label and icon in each other's slots

## [1.4.0] - 2026-09-10

### Added
- Drop sets, supersets and rest timer with a notification
- Plan presets: target sets, rep range, rest and weight per exercise
- Session summary: tonnage, worked muscles, name, note, photo and duration
- Records by one-rep max, last-session ghost and a gym-to-gym weight shift
- Statistics with a body heat map, muscle and exercise screens, CSV and PDF reports
- Exercise library of 876 movements with illustrations from a CDN
- Gyms with their own equipment, and grips that belong to an exercise
- Plate calculator, weight entry by keyboard, kilograms or pounds
- Session history grouping, moving and deleting a session, plan and day copying

### Changed
- Plans are optional: a session runs without one
- Plan days are plain numbers, week mode is a setting
- Set rows carry the kind of set and a note about the weight
- Every interface string starts with a capital letter

### Fixed
- The weight ruler wrote 75.4 while pointing at 75
- The plate calculator doubled every plate and would not let one off
- The rest panel took half the screen and slid under the phone buttons
- The add button covered the exercise and superset actions in the day editor

## [1.3.3] - 2026-04-11

### Added
- Backup and restore for whole data
- Activity graph on the Profile screen
- Support for 35 additional locales
- Plan Edit button in session detail

### Changed
- Removed Rest timer (will be replaced soon)
- Updated Plan Edit page design
- Better animations

## [1.3.2] - 2025-11-13

### Added
- Session History card on Home screen
- Set Type selection for sets
- Timer which shows time since last set
- Monochrome launcher icon on Android 12+
- Allow adding a new exercise directly when it cannot be found in the list
- Clean up empty plans from the Plans screen via confirmation dialog
- Enable predictive back navigation
- Empty state on Sessions screen

### Changed
- Removed Bottom navigation bar, added user icon to top bar
- Updated Day Switcher component styling for clarity
- Hide Lifts card when there are no lifts
- Redesigned Session History card and screen
- Replaced profile icon on Home screen
- Removed "Today" label on Sessions and filtered out empty sessions

### Fixed
- Select Plan button alignment
- Prevent unintended translation for Turkish app name
- Session list now shows most recent first
- Lifts card not showing even when lifts existed
- Sets from past sessions not shown when the exercise was removed from the corresponding plan

## [1.3.0] - 2025-01-17

### Added
- Drag text field in "Add Set"
- Double tap to edit "set info"
- History Icon (You can check last week's session if it exists)
- Support for Monochrome icon on Android 12+
- Text animation on Onboarding
- Safer way to delete Sets / Exercises / Plans
- New Font for headings

### Changed
- Targets Android 15
- Onboarding screen
- Default theme for new users
- Sorting of muscle groups chips
- Always save plan on going back
- Color in Profile
- Home Screen and On-boarding Screen
- Some buttons and UI elements

### Fixed
- Save button not visible
- Two `Default` theme in Settings
- Scrolling on `Select Exercise` Sheet
- Performance issues on `Add Set` Sheet
- Weird line in the setting wave
- Crash on deleting plan
- On boarding not completing
- Loads of performance improvements

### Removed
- Gradient in settings

## [1.2.0] - 2024-05-26

### Added
- Support for isometric exercises
- Deleting Sets / Exercises / Plans

### Changed
- Error message height
- Chips type in `Select Exercise`

### Fixed
- Navigation to same page again
- Double back presses
- Swipe gesture on reps and weight text field
- Elements squashing on small screens
- Empty exercises
- Invalid reference
- False reference icon

## [1.1.1] - 2024-05-19

### Fixed
- Navigation from home screen
- Annoying animations on home page
- Plan Edit Page
- Back button on all pages

## [1.1.0] - 2024-05-19

### Added
- New Home Page
- Back button on Exercises Page
- Option to open References from workout page(if added)

### Changed
- Splash Screen Image to reduce dependency on `NonFreeNet`
- Whole Plan card is clickable

### Fixed
- APK dependency tree encryption
- Color of icons on some buttons
- `Zestful` Color Palettes
- Crash when using invalid reference
- UI/UX for Exercises Page
- Some navigation crashes

## [1.0.0] - 2024-05-12

### Added
- Initial Release
