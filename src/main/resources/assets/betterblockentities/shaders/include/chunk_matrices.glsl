/* the projection matrix */
uniform mat4 u_ProjectionMatrix;

/* the model-view matrix */
uniform mat4 u_ModelViewMatrix;

/* the model-view-projection matrix */
#define u_ModelViewProjectionMatrix (uProjectionMatrix * u_ModelViewMatrix)