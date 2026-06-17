///////////////////////////////////////////////////////////////////////////////
// viewmanager.h
// ============
// manage the viewing of 3D objects within the viewport - camera, projection
//
//  AUTHOR: Brian Battersby - SNHU Instructor / Computer Science
//	Created for CS-330-Computational Graphics and Visualization, Nov. 1st, 2023
///////////////////////////////////////////////////////////////////////////////

#pragma once

#include "ShaderManager.h"
#include "camera.h"

// GLFW library
#include "GLFW/glfw3.h" 

class ViewManager
{
public:
	// constructor
	ViewManager(
		ShaderManager* pShaderManager);
	// destructor
	~ViewManager();

	// mouse position callback for mouse interaction with the 3D scene
	static void Mouse_Position_Callback(GLFWwindow* window, double xMousePos, double yMousePos);
	// mouse scroll callback used for adjusting camera travel speed
	static void Mouse_Scroll_Callback(GLFWwindow* window, double xOffset, double yOffset);
	// framebuffer resize callback used to keep glViewport in sync with window dimensions
	static void Framebuffer_Size_Callback(GLFWwindow* window, int width, int height);

private:
	// pointer to shader manager object
	ShaderManager* m_pShaderManager;
	// active OpenGL display window
	GLFWwindow* m_pWindow;

	// process keyboard events for interaction with the 3D scene
	void ProcessKeyboardEvents();

public:
	// create the initial OpenGL display window
	GLFWwindow* CreateDisplayWindow(const char* windowTitle);

	// allow the main loop (or other managers) to query which projection is active
	bool IsOrthographicProjectionEnabled() const;
	
	// prepare the conversion from 3D object display to 2D scene display
	void PrepareSceneView();
};