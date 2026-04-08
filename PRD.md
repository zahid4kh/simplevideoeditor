# Product Requirement Document (PRD) - Simple Video Editor

## 1. Overview
The goal of this project is to build a simple, lightweight video editing desktop application using **Compose for Desktop**. The app will allow users to upload, preview, and perform basic editing on MP4 video files.

## 2. Target Audience
Users looking for a straightforward, "Capcut-like" dashboard for quick video editing without the complexity of professional suites.

## 3. Technology Stack
- **UI Framework**: Compose for Desktop (Material3).
- **Video Playback/Visualization**: `vlcj` (VLC for Java).
- **Video Processing**: FFmpeg (via Java `ProcessBuilder`).
- **File Selection**: `Deskit` library (`FileChooserDialog`).
- **Dependencies**: VLC and FFmpeg must be pre-installed on the host system.

## 4. Key Features

### 4.1. Dashboard UI
- **Layout**: A modern, dark-themed dashboard inspired by Capcut.
- **Video Canvas**: A central area for video playback that automatically adapts to the video's dimensions (maintaining aspect ratio).
- **Sidebar/Toolbar**: Controls for uploading and basic editing actions.

### 4.2. Video Upload & Selection
- **Integration**: Use `Deskit`'s `FileChooserDialog` for selecting MP4 files.
- **Code Reference**:
  ```kotlin
  FileChooserDialog(
      onCancel = { /* Handle cancel */ },
      onFileSelected = { file -> /* Load video */ }
  )
  ```

### 4.3. Video Timeline
- **Visuals**: A horizontal timeline displaying the video track.
- **Vertical Playhead**: A track that moves automatically during playback or can be manually dragged by the user to scrub through frames.
- **Metadata Display**:
    - Video Duration.
    - File Name.
    - File Size.
    - FPS (Frames Per Second).

### 4.4. Video Editing
- **Mechanism**: All editing operations (e.g., trimming, cutting, merging) will be processed using FFmpeg through `ProcessBuilder`.
- **Visualization**: Real-time (or near real-time) feedback in the `vlcj` player.

## 5. User Workflow
1. User opens the app.
2. User clicks "Upload" and selects an MP4 video using the Deskit file chooser.
3. The video loads into the player canvas and the timeline populates with metadata.
4. User can play/pause the video or scrub using the timeline playhead.
5. User performs editing actions (to be defined in detail).
6. User exports the edited video.

## 6. UI/UX Requirements
- Strictly follow **Material3** design principles.
- Responsive layout to handle different window sizes.
- Intuitive drag-and-drop or click-to-scrub timeline interactions.

## 7. Success Metrics
- Seamless integration of VLC for low-latency playback.
- Accurate metadata extraction and display.
- Reliable execution of FFmpeg commands for editing tasks.
