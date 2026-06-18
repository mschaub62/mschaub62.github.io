///////////////////////////////////////////////////////////////////////////////
// viewmanager.cpp
// ============
// manage the viewing of 3D objects within the viewport - camera, projection
//
//  AUTHOR: Brian Battersby - SNHU Instructor / Computer Science
//	Created for CS-330-Computational Graphics and Visualization, Nov. 1st, 2023
///////////////////////////////////////////////////////////////////////////////

#include "ViewManager.h"

#include <iostream>

// GLM Math Header inclusions
#include <glm/gtx/transform.hpp>
# Install vcpkg if you haven't
vcpkg install glm

# For x64
vcpkg install glm:x64-windows
vcpkg integrate install
find_package(glm CONFIG REQUIRED)
target_link_libraries(your_target_name PRIVATE glm::glm)
include_directories(${GLM_INCLUDE_DIRS})
#include <glm/gtc/matrix_transform.hpp> <glm/gtc/matrix_transform.hpp>
#include <glm/gtc/type_ptr.hpp>    
#include <glm/glm.hpp> // Add this line

// declaration of the global variables and defines
namespace
{
	// Variables for window width and height
	const int WINDOW_WIDTH = 1000;
	const int WINDOW_HEIGHT = 800;

	// track the current framebuffer size (used for glViewport and aspect ratio)
	int gFramebufferWidth = WINDOW_WIDTH;
	int gFramebufferHeight = WINDOW_HEIGHT;
	const char* g_ViewName = "view";
	const char* g_ProjectionName = "projection";

	// camera object used for viewing and interacting with
	// the 3D scene
	Camera* g_pCamera = nullptr;

	// these variables are used for mouse movement processing
	float gLastX = WINDOW_WIDTH / 2.0f;
	float gLastY = WINDOW_HEIGHT / 2.0f;
	bool gFirstMouse = true;

	// time between current frame and last frame
	float gDeltaTime = 0.0f; 
	float gLastFrame = 0.0f;

	// the following variable is false when orthographic projection
	// is off and true when it is on
	bool bOrthographicProjection = false;

	// the following variable is true when the cursor is DISABLED (i.e., 
	// it is hidden and bound to the window); otherwise it is NORMAL 
	// (i.e., unconstrained to the window). 
	bool bCursorDisabled = true;

	// a boolean to note that the TAB key has been pressed (cleared 
	// after a release)
	bool bTabPressed = false;

	// a boolean to note that the O or P key has been pressed (cleared after release)
	bool bOPressed = false;
	bool bPPressed = false;

	// camera travel speed constraints (mouse wheel adjusts within this range)
	const float MIN_CAMERA_SPEED = 2.0f;
	const float MAX_CAMERA_SPEED = 80.0f;
}

/***********************************************************
 *  ViewManager()
 *
 *  The constructor for the class
 ***********************************************************/
ViewManager::ViewManager(
	ShaderManager *pShaderManager)
{
	// initialize the member variables
	m_pShaderManager = pShaderManager;
	m_pWindow = NULL;
	g_pCamera = new Camera();
	// default camera view parameters
	g_pCamera->Position = glm::vec3(0.0f, 5.0f, 12.0f);
	g_pCamera->Front = glm::vec3(0.0f, -0.5f, -2.0f);
	g_pCamera->Up = glm::vec3(0.0f, 1.0f, 0.0f);
	g_pCamera->Zoom = 80;
	g_pCamera->MovementSpeed = 20;
}

/***********************************************************
 *  ~ViewManager()
 *
 *  The destructor for the class
 ***********************************************************/
ViewManager::~ViewManager()
{
	// free up allocated memory
	m_pShaderManager = NULL;
	m_pWindow = NULL;
	if (NULL != g_pCamera)
	{
		delete g_pCamera;
		g_pCamera = NULL;
	}
}

/***********************************************************
 *  CreateDisplayWindow()
 *
 *  This method is used to create the main display window.
 ***********************************************************/
GLFWwindow* ViewManager::CreateDisplayWindow(const char* windowTitle)
{
	GLFWwindow* window = nullptr;

	// try to create the displayed OpenGL window
	window = glfwCreateWindow(
		WINDOW_WIDTH,
		WINDOW_HEIGHT,
		windowTitle,
		NULL, NULL);
	if (window == NULL)
	{
		std::cout << "Failed to create GLFW window" << std::endl;
		glfwTerminate();
		return NULL;
	}
	glfwMakeContextCurrent(window);

	// Set the initial OpenGL viewport. (Rubric hint: glViewport.)
	// If the window is resized, the framebuffer callback below keeps this in sync.
	glViewport(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

	// keep the viewport synchronized to window resizes
	glfwSetFramebufferSizeCallback(window, &ViewManager::Framebuffer_Size_Callback);

	// tell GLFW to capture all mouse events
	glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

	// this callback is used to receive mouse moving events
	glfwSetCursorPosCallback(window, &ViewManager::Mouse_Position_Callback);
	// this callback is used to receive mouse wheel events (adjust camera travel speed)
	glfwSetScrollCallback(window, &ViewManager::Mouse_Scroll_Callback);

	// enable blending for supporting tranparent rendering
	glEnable(GL_BLEND);
	glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

	m_pWindow = window;

	return(window);
}

/***********************************************************
 *  Framebuffer_Size_Callback()
 *
 *  This method is automatically called from GLFW whenever
 *  the framebuffer size changes (e.g., window resize). This
 *  keeps glViewport and aspect ratio calculations correct.
 ***********************************************************/
void ViewManager::Framebuffer_Size_Callback(GLFWwindow* window, int width, int height)
{
	// Protect against divide-by-zero when the window is minimized
	if (width <= 0 || height <= 0)
	{
		return;
	}

	gFramebufferWidth = width;
	gFramebufferHeight = height;
	glViewport(0, 0, width, height);
}

/***********************************************************
 *  Mouse_Position_Callback()
 *
 *  This method is automatically called from GLFW whenever
 *  the mouse is moved within the active GLFW display window.
 ***********************************************************/
void ViewManager::Mouse_Position_Callback(GLFWwindow* window, double xMousePos, double yMousePos)
{
	// only allow mouse-look when the cursor is captured (disabled)
	// this prevents unwanted camera rotation while the cursor is free
	if (!bCursorDisabled)
	{
		return;
	}

	// when the first mouse move event is received, this needs to be recorded so that
	// all subsequent mouse moves can correctly calculate the X position offset and Y
	// position offset for proper operation
	if (gFirstMouse)
	{
		gLastX = xMousePos;
		gLastY = yMousePos;
		gFirstMouse = false;
	}

	// calculate the X offset and Y offset values for moving the 3D camera accordingly
	float xOffset = xMousePos - gLastX;
	float yOffset = gLastY - yMousePos; // reversed since y-coordinates go from bottom to top

	// set the current positions into the last position variables
	gLastX = xMousePos;
	gLastY = yMousePos;

	// move the 3D camera according to the calculated offsets
	g_pCamera->ProcessMouseMovement(xOffset, yOffset);
}

/***********************************************************
 *  Mouse_Scroll_Callback()
 *
 *  This method is automatically called from GLFW whenever
 *  the mouse wheel is scrolled. In this milestone, the mouse
 *  wheel adjusts the travel speed of the camera.
 ***********************************************************/
void ViewManager::Mouse_Scroll_Callback(GLFWwindow* window, double xOffset, double yOffset)
{
	if (NULL == g_pCamera)
	{
		return;
	}

	// scroll up -> faster, scroll down -> slower
	// use an additive step so each scroll tick produces a predictable change
	const float SPEED_STEP = 2.5f;
	float newSpeed = g_pCamera->MovementSpeed + static_cast<float>(yOffset) * SPEED_STEP;

	// clamp speed to avoid values that are too slow or too fast
	if (newSpeed < MIN_CAMERA_SPEED) newSpeed = MIN_CAMERA_SPEED;
	if (newSpeed > MAX_CAMERA_SPEED) newSpeed = MAX_CAMERA_SPEED;

	g_pCamera->MovementSpeed = newSpeed;
}

/***********************************************************
 *  ProcessKeyboardEvents()
 *
 *  This method is called to process any keyboard events
 *  that may be waiting in the event queue.
 ***********************************************************/
void ViewManager::ProcessKeyboardEvents()
{
	// close the window if the escape key has been pressed
	if (glfwGetKey(m_pWindow, GLFW_KEY_ESCAPE) == GLFW_PRESS)
	{
		glfwSetWindowShouldClose(m_pWindow, true);
	}
	
	// enable or disable the cursor to the window
	// code has been updated to release one ball per spacebar press and release
	if (glfwGetKey(m_pWindow, GLFW_KEY_TAB) == GLFW_PRESS)
	{
		bTabPressed = true;
	}
	if (bTabPressed && (glfwGetKey(m_pWindow, GLFW_KEY_TAB) == GLFW_RELEASE))
	{
		bTabPressed = false;
		if (bCursorDisabled)
		{
			glfwSetInputMode(m_pWindow, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
		}
		else 
		{
			glfwSetInputMode(m_pWindow, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
		}
		// change the variable to the new state
		bCursorDisabled = !bCursorDisabled;
		// reset first-mouse so re-capturing the cursor does not cause a sudden camera jump
		gFirstMouse = true;
	}

	// projection toggles (tap P for perspective, O for orthographic)
	if (glfwGetKey(m_pWindow, GLFW_KEY_O) == GLFW_PRESS)
	{
		bOPressed = true;
	}
	if (bOPressed && (glfwGetKey(m_pWindow, GLFW_KEY_O) == GLFW_RELEASE))
	{
		bOPressed = false;
		bOrthographicProjection = true;
	}

	if (glfwGetKey(m_pWindow, GLFW_KEY_P) == GLFW_PRESS)
	{
		bPPressed = true;
	}
	if (bPPressed && (glfwGetKey(m_pWindow, GLFW_KEY_P) == GLFW_RELEASE))
	{
		bPPressed = false;
		bOrthographicProjection = false;
	}

	// camera translation controls
	// - WASD: forward/back/left/right
	// - QE: up/down
	// speed is controlled by g_pCamera->MovementSpeed and the mouse wheel callback
	if (NULL != g_pCamera)
	{
		const float velocity = g_pCamera->MovementSpeed * gDeltaTime;
		const glm::vec3 worldUp(0.0f, 1.0f, 0.0f);
		const glm::vec3 right = glm::normalize(glm::cross(g_pCamera->Front, g_pCamera->Up));

		// forward/back
		if (glfwGetKey(m_pWindow, GLFW_KEY_W) == GLFW_PRESS)
		{
			g_pCamera->Position += g_pCamera->Front * velocity;
		}
		if (glfwGetKey(m_pWindow, GLFW_KEY_S) == GLFW_PRESS)
		{
			g_pCamera->Position -= g_pCamera->Front * velocity;
		}

		// left/right
		if (glfwGetKey(m_pWindow, GLFW_KEY_A) == GLFW_PRESS)
		{
			g_pCamera->Position -= right * velocity;
		}
		if (glfwGetKey(m_pWindow, GLFW_KEY_D) == GLFW_PRESS)
		{
			g_pCamera->Position += right * velocity;
		}

		// up/down
		if (glfwGetKey(m_pWindow, GLFW_KEY_Q) == GLFW_PRESS)
		{
			g_pCamera->Position += worldUp * velocity;
		}
		if (glfwGetKey(m_pWindow, GLFW_KEY_E) == GLFW_PRESS)
		{
			g_pCamera->Position -= worldUp * velocity;
		}
	}
}

/***********************************************************
 *  PrepareSceneView()
 *
 *  This method is used for preparing the 3D scene by loading
 *  the shapes, textures in memory to support the 3D scene 
 *  rendering
 ***********************************************************/
void ViewManager::PrepareSceneView()
{
	glm::mat4 view;
	glm::mat4 projection;
	glm::vec3 cameraPosForLighting;

	// per-frame timing
	float currentFrame = glfwGetTime();
	gDeltaTime = currentFrame - gLastFrame;
	gLastFrame = currentFrame;

	// process any keyboard events that may be waiting in the 
	// event queue
	ProcessKeyboardEvents();

	// get the current view matrix from the camera
	// for perspective mode, we use the dynamic camera view (mouse + keyboard)
	// for orthographic mode, we use a fixed "front" view of the main object
	// (rubric hint: glOrtho -> modern equivalent is glm::ortho)
	float aspectRatio = 1.0f;
	if (gFramebufferHeight > 0)
	{
		aspectRatio = static_cast<float>(gFramebufferWidth) / static_cast<float>(gFramebufferHeight);
	}

	if (bOrthographicProjection)
	{
		// Fixed orthographic view (front view). Update these if your scene's focal point changes.
		const glm::vec3 ORTHO_TARGET(0.0f, 2.0f, 2.0f);
		const glm::vec3 ORTHO_POSITION(0.0f, 2.5f, 15.0f);
		const glm::vec3 ORTHO_UP(0.0f, 1.0f, 0.0f);

		view = glm::lookAt(ORTHO_POSITION, ORTHO_TARGET, ORTHO_UP);
		cameraPosForLighting = ORTHO_POSITION;

		// Ortho volume size (world units). Smaller values "zoom in" the ortho view.
		const float ORTHO_SCALE = 6.0f;
		projection = glm::ortho(
			-ORTHO_SCALE * aspectRatio,
			 ORTHO_SCALE * aspectRatio,
			-ORTHO_SCALE,
			 ORTHO_SCALE,
			0.1f,
			100.0f);
	}
	else
	{
		view = g_pCamera->GetViewMatrix();
		projection = glm::perspective(glm::radians(g_pCamera->Zoom), aspectRatio, 0.1f, 100.0f);
		cameraPosForLighting = g_pCamera->Position;
	}

	// if the shader manager object is valid
	if (NULL != m_pShaderManager)
	{
		// set the view matrix into the shader for proper rendering
		m_pShaderManager->setMat4Value(g_ViewName, view);
		// set the view matrix into the shader for proper rendering
		m_pShaderManager->setMat4Value(g_ProjectionName, projection);
		// set the view position of the camera into the shader for proper rendering
		m_pShaderManager->setVec3Value("viewPosition", cameraPosForLighting);
	}
}

bool ViewManager::IsOrthographicProjectionEnabled() const
{
	return bOrthographicProjection;
}