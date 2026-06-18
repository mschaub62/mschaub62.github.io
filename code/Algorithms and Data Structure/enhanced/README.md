# Algorithms & Data Structures — OpenGL 3D Scene (Enhanced)

**Artifact:** 3D scene renderer originally built for CS-330 (Computational Graphics &
Visualization).
**Language / tools:** C++, OpenGL, GLSL, GLEW/GLFW/GLM, Visual Studio.

This folder contains the **enhanced** version of the project. The original, pre-enhancement
source is in `../original/`. This document lists exactly what changed and why.

---

## Summary of changes

| Area | Change | File |
|------|--------|------|
| Correctness | Orthographic projection state is tracked and used instead of being hardcoded | `SceneManager.cpp/.h`, `ViewManager.cpp` |
| Memory safety | Texture cleanup now releases GPU textures instead of allocating new ones | `SceneManager.cpp` |
| Memory safety | Image data is freed on the unsupported-format error path | `SceneManager.cpp` |
| Defensive code | Boundary check prevents writing past the fixed 16-slot texture array | `SceneManager.cpp` |

All four changes are localized and do not alter the visual scene; they fix logic,
resource handling, and safety behind it.

---

## 1. Orthographic projection logic

The original `SceneManager` reported its projection mode with a method that always
returned a constant, so the rest of the renderer could never tell when orthographic mode
was active.

```cpp
// ORIGINAL — SceneManager.cpp
bool SceneManager::IsOrthographicProjectionEnabled() const
{
    return false;          // ignores the real state
}
```

The enhanced version stores the real projection state in a member variable
(`m_isOrthographicProjectionEnabled`), initializes it in the constructor, updates it
through `SetOrthographicProjectionEnabled(bool)`, and actually uses it when rendering
(for example, to hide the ground plane in orthographic mode):

```cpp
// ENHANCED — SceneManager.cpp
m_isOrthographicProjectionEnabled = false;            // constructor
...
void SceneManager::SetOrthographicProjectionEnabled(bool enabled)
{
    m_isOrthographicProjectionEnabled = enabled;
}
...
if (!m_isOrthographicProjectionEnabled) { /* draw ground plane */ }
```

In `ViewManager`, the `P` and `O` keys toggle perspective and orthographic projection,
and the stored flag drives the projection matrix.

## 2. Texture cleanup (memory safety)

The original cleanup routine called `glGenTextures` — which **creates** textures — inside
a loop that was supposed to **free** them. This leaked every texture and never reset the
counter.

```cpp
// ORIGINAL — leaks GPU resources
void SceneManager::DestroyGLTextures()
{
    for (int i = 0; i < m_loadedTextures; i++)
        glGenTextures(1, &m_textureIDs[i].ID);   // wrong call
}
```

```cpp
// ENHANCED — releases and resets each slot
void SceneManager::DestroyGLTextures()
{
    for (int i = 0; i < m_loadedTextures; i++)
    {
        glDeleteTextures(1, &m_textureIDs[i].ID);
        m_textureIDs[i].ID = 0;
        m_textureIDs[i].tag = "";
    }
    m_loadedTextures = 0;
}
```

## 3. Boundary check on the fixed texture array

Textures are stored in a fixed array, `TEXTURE_INFO m_textureIDs[16]`. The original code
wrote a newly loaded texture into `m_textureIDs[m_loadedTextures]` with no check, so
loading a 17th texture would write past the end of the array (undefined behavior). The
enhanced version refuses to overflow the array and reports the problem:

```cpp
// ENHANCED — guard before registering a texture
if (m_loadedTextures >= 16)
{
    std::cout << "Texture array is full (16 max); cannot load: " << filename << std::endl;
    glDeleteTextures(1, &textureID);   // don't leak the texture we just made
    return false;
}
m_textureIDs[m_loadedTextures].ID = textureID;
m_textureIDs[m_loadedTextures].tag = tag;
m_loadedTextures++;
```

## 4. Free image memory on the error path

When an image has an unsupported channel count, the function returns early. The original
returned **before** releasing the decoded image buffer, leaking it. The enhanced version
frees it first:

```cpp
// ENHANCED
else
{
    std::cout << "Not implemented to handle image with " << colorChannels << " channels" << std::endl;
    stbi_image_free(image);   // release image memory before returning
    return false;
}
```

---

## Course outcomes addressed

- **Outcome 3 (strongest):** uses the fixed-size texture array and structured rendering
  logic to solve a graphics problem, handling trade-offs so the corrections do not break
  the existing scene.
- **Outcome 4:** applies C++, OpenGL, GLSL, and Visual Studio to improve a working
  application.
- **Outcome 5:** defensive programming — boundary checking, correct resource release, and
  safe handling of invalid texture data.

## Building

Open the project in Visual Studio with the OpenGL dependencies (GLEW, GLFW, GLM, stb_image)
configured for the solution, then build and run. The `P` and `O` keys toggle perspective
and orthographic projection; mouse and `W/A/S/D` control the camera.

> **Note:** after pulling these changes, rebuild in Visual Studio to confirm everything
> compiles in your environment before submitting.
