# Matthew Schaub — Computer Science ePortfolio

Capstone ePortfolio for the B.S. in Computer Science (SNHU, CS 499). It presents a
professional self-assessment, an informal code review, and three enhanced artifacts
spanning **software design & engineering**, **algorithms & data structures**, and
**databases**.

**Live site:** once deployed, `https://github.com/mschaub62/mschaub62.github.io`

---

## Deploy to GitHub Pages

1. Create a new GitHub repository (public).
2. Upload the entire contents of this `eportfolio/` folder to the repository root
   so that `index.html` sits at the top level of the repo.
3. In the repository, go to **Settings → Pages**.
4. Under **Build and deployment**, set **Source** to *Deploy from a branch*, choose
   the `main` branch and the `/ (root)` folder, then **Save**.
5. Wait a minute, then open `https://<your-username>.github.io/<repo-name>/`.

The `.nojekyll` file tells GitHub Pages to serve every file as-is, so the code folders
and their listing pages work without any build step.

---

## Structure

```
index.html                          Home: self-assessment, code review, artifact gallery
software-design-engineering.html    SDE·01 — Weight Tracking App (Java / Android)
algorithms-data-structures.html     ALG·02 — OpenGL 3D Scene (C++ / OpenGL)
databases.html                      DAT·03 — Shelter Dashboard (Python / MongoDB)
assets/
  styles.css                        Shared design system
  main.js                           Navigation, scroll reveal
code/
  software-design-engineering/{original,enhanced}/
  algorithms-data-structures/{original,enhanced}/
  databases/{original,enhanced}/
```

Each `code/.../` folder has an `index.html` that lists its source files, so the
**View code** buttons on the artifact pages open a readable file index on the live site.

---

## Three things to finish before you publish

### 1. Add your code-review video
Open `index.html` and find the `#review` section. Replace the `.video__screen`
placeholder with **one** of:

```html
<!-- Unlisted YouTube video -->
<div class="video__screen">
  <iframe src="https://www.youtube.com/embed/VIDEO_ID"
          title="Code review" allowfullscreen></iframe>
</div>
```

```html
<!-- Local file: drop code-review.mp4 next to index.html -->
<div class="video__screen">
  <video controls src="code-review.mp4"></video>
</div>
```

### 2. Supply the two missing "original" sources
The uploaded archives did not contain usable original source for two artifacts, so
those folders currently show a placeholder:

- `code/software-design-engineering/original/` — the original archive held only build
  output. Add the original Android Java source (`app/src/main/java/...`).
- `code/algorithms-data-structures/original/` — no separate original archive was
  provided. Add the original CS-330 `Source/` and `shaders/` files.

After adding files, the listing pages regenerate themselves only if you re-run the
generator; the simplest path is to keep the existing `index.html` and just confirm the
file links resolve, or replace that folder's `index.html` with a short note linking to
the files.

### 3. Rotate the database password
The uploaded database archive included a real `.env` with a MongoDB password. That file
is **intentionally excluded** from this repo (only `.env.example` ships). If that
password was ever used on a live database, rotate it — the enhanced project includes
`code/databases/enhanced/rotate_mongo_password.py` for exactly that.

---

## Running the artifacts locally (optional)

- **Database dashboard:** `cd code/databases/enhanced`, copy `.env.example` to `.env`,
  fill in your values, `pip install -r requirements.txt`, then `python ProjectTwoDashboard_Enhanced.py`.
- **Weight Tracking App:** open the original Android project in Android Studio and build.
- **OpenGL scene:** open the project in Visual Studio with the OpenGL/GLM/GLFW
  dependencies configured, and build.

---

© Matthew Schaub · SNHU CS 499 · 2026
