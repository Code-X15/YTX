
# 🎧 YTX Server

[![Python](https://img.shields.io/badge/Python-3.9%2B-blue)](https://www.python.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-%E2%9C%85-brightgreen)](https://fastapi.tiangolo.com/)
[![yt-dlp](https://img.shields.io/badge/yt--dlp-%E2%9C%85-orange)](https://github.com/yt-dlp/yt-dlp)
[![Download APK](https://img.shields.io/badge/Download-YTX_v1.0.0-brightgreen)](https://github.com/Code-X15/YTX/releases/download/v1.0.0/YTXv1.0.0.apk)

YTX Server is a **lightweight FastAPI + yt-dlp backend** for downloading YouTube videos or audio with **live progress updates**.  
It powers the **YTX Downloader App** for Android.

---

## 🚀 Features

- Download **MP3 audio** or **MP4 video**  
- **Live download progress** via WebSocket  
- Automatic **filename cleaning**  
- **Thumbnail embedding**  
- **Metadata embedding** (Title + Artist)  
- Safe filenames (no Windows-breaking characters)  

---

## 📦 Requirements

- **Python 3.9+**  
- **FFmpeg** installed and added to system PATH  
  Download: [FFmpeg](https://ffmpeg.org/download.html)  

---


## 📸 App Screenshots

| Home Screen | Link Box Preview | Loading Metadata | Metadata Preview | Download Screen | Settings Screen |
|------------|----------------|----------------|----------------|----------------|----------------|
| ![Download Screen](https://github.com/Code-X15/YTX/raw/main/server/images/download_screen.png) | ![Example Link](https://github.com/Code-X15/YTX/raw/main/server/images/l.png) | ![Loading MetaData](https://github.com/Code-X15/YTX/raw/main/server/images/lm.png) | ![MetaData Preview](https://github.com/Code-X15/YTX/raw/main/server/images/mp.png) | ![Download Screen](https://github.com/Code-X15/YTX/raw/main/server/images/d.png) | ![Settings Screen](https://github.com/Code-X15/YTX/raw/main/server/images/settings_screen.png) |
---
---

## 🛠 Python Dependencies

```bash
pip install fastapi uvicorn yt-dlp mutagen requests
pip install yt-dlp[default]  # for full YouTube support
````

---

## 📥 Downloading the Server

Clone the repository:

```bash
git clone REPO_LINK
cd project-folder
```

Or download ZIP from GitHub → Extract → Open folder.

### 📁 Project Structure

```
project/
│
├── s2.py             # Main FastAPI server
├── downloads/        # Folder to store downloaded files
└── README.md
```

---

## ▶️ Running the Server

Open terminal inside the project folder:

```bash
uvicorn s2:app --host 0.0.0.0 --port 8000
```

You should see:

```
Uvicorn running on http://0.0.0.0:8000
```

Your server is now live.

---

## 🔌 Server Endpoints

### Ping Server

**GET** `/ping`

**Response:**

```json
{"status": "online"}
```

### Get Video Metadata

**GET** `/metadata?url=VIDEO_URL`

Example:

```
http://localhost:8000/metadata?url=https://youtu.be/dQw4w9WgXcQ
```

**Response:**

```json
{
  "title": "Never Gonna Give You Up",
  "thumbnail": "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
  "author_name": "Rick Astley",
  "duration": 213
}
```

### Start Download (WebSocket)

**ws://localhost:8000/download**

Send JSON:

```json
{
  "url": "VIDEO_URL",
  "format": "mp3"
}
```

Supported formats: `mp3`, `mp4`
Live progress will be sent over the WebSocket.

### Download Finished File

**GET** `/download_file?filename=FILE_NAME`

Example:

```
http://localhost:8000/download_file?filename=NeverGonnaGiveYouUp.mp3
```

---

## 📂 File Storage

All downloads are saved in the `downloads/` folder.

**Filename cleaning example:**

Original:

```
Example | Slowed+Reverb 🎶
```

Becomes:

```
Example Slowed Reverb.mp3
```

---

## 📱 Installing the YTX App

1. Go to GitHub → Releases
2. Download the latest APK: [YTX_v1.0.0.apk](https://github.com/Code-X15/YTX/releases/download/v1.0.0/YTXv1.0.0.apk)
3. Install on Android
4. Allow "Unknown Sources" if blocked

> ⚠️ This app requires a personal server to function.

---

## ⚠️ Troubleshooting

* **FFmpeg not found** → Make sure FFmpeg is installed and added to PATH.
* **yt-dlp extraction warning** → Run:

```bash
pip install yt-dlp[default]
```

---

## 💡 About

Built with:

* FastAPI
* yt-dlp
* Mutagen
* WebSockets

Provides a fast backend for YouTube downloads with live progress updates.

---

## ❤️ Credits

Created by **CodeX**
Part of the **YTX Project**



