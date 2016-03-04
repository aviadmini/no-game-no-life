package com.aviadmini.nogamenolife.opengl;

import android.opengl.GLES20;
import android.opengl.GLU;

import timber.log.Timber;

/**
 * <p> Utility class for OpenGL </p>
 */
public class UtilsGL {

    private static final String TAG = "UtilsGL";

    /**
     * <p>Identity transformation matrix</p>
     */
    private static final float[] IDENTITY_MATRIX = new float[]{//
            1, 0, 0, 0, //
            0, 1, 0, 0,//
            0, 0, 1, 0, //
            0, 0, 0, 1};

    public static float[] getIdentityMatrix() {
        return IDENTITY_MATRIX.clone();
    }

    /**
     * <p>Quad triangle strip for vertex and texture attribute arrays (non-FBO version)</p>
     */
    private static final float[] QUAD = {//
            1.0f, 1.0f, 1.0f, 0.0f,// top right
            -1.0f, 1.0f, 0.0f, 0.0f,// top left
            1.0f, -1.0f, 1.0f, 1.0f,// bottom right
            -1.0f, -1.0f, 0.0f, 1.0f,// bottom left
    };

    /**
     * <p>Quad triangle strip for vertex and texture attribute arrays (FBO version)</p>
     */
    private static final float[] QUAD_FBO = {//
            1.0f, 1.0f, 1.0f, 1.0f,//
            -1.0f, 1.0f, 0.0f, 1.0f,//
            1.0f, -1.0f, 1.0f, 0.0f,//
            -1.0f, -1.0f, 0.0f, 0.0f,//
    };

    public static float[] getQuad() {
        return QUAD.clone();
    }

    public static float[] getQuadFbo() {
        return QUAD_FBO.clone();
    }

    /**
     * <p> 4-byte allocation needed for float type </p>
     */
    public static final int BYTES_PER_FLOAT = 4;
    /**
     * <p> 2-byte allocation needed for short type </p>
     */
    public static final int BYTES_PER_SHORT = 2;

    /**
     * <p> Checks given integer for being power of two </p>
     *
     * @param pNumber integer to check for being power of two
     * @return true if supplied integer is power of two, false otherwise
     */
    public static boolean isPowerOfTwo(final int pNumber) {
        return (pNumber & -pNumber) == pNumber;
    }

    /**
     * <p> Queries OpenGL for errors and if there have been some - shows all found information in logs </p>
     *
     * @param pMessage additional message to show in logs if error found
     */
    public static void logErrorGL(final String pMessage) {

        final int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {

            Timber.tag(TAG);
            Timber.e("%s Opengl error %s: %s", (pMessage == null ? "" : pMessage + " >>> "), error, GLU.gluErrorString(error));

        }

    }

    /**
     * <p> Queries OpenGL for errors and if there have been some - shows all found information in logs </p>
     */
    public static void logErrorGL() {
        logErrorGL(null);
    }

    /**
     * <p>Creates shader program, attaches shaders compiled from given source codes & links shader program</p>
     *
     * @param pVertexShaderSource   Vertex shader source code
     * @param pFragmentShaderSource Fragment shader source code
     * @return shader program handle or 0 if it could not be
     */
    public static int createShaderProgram(final String pVertexShaderSource, final String pFragmentShaderSource) {

        final int vertexShaderHandle = loadShader(true, pVertexShaderSource);
        final int fragmentShaderHandle = loadShader(false, pFragmentShaderSource);

        if (vertexShaderHandle == 0 || fragmentShaderHandle == 0) {
            return 0;
        }

        int shaderProgramHandle = GLES20.glCreateProgram();

        GLES20.glAttachShader(shaderProgramHandle, vertexShaderHandle);
        GLES20.glAttachShader(shaderProgramHandle, fragmentShaderHandle);

        GLES20.glLinkProgram(shaderProgramHandle);

        final int[] linked = new int[1];
        GLES20.glGetProgramiv(shaderProgramHandle, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {

            Timber.tag(TAG);
            Timber.e("Could not link program: ");
            Timber.e(GLES20.glGetProgramInfoLog(shaderProgramHandle));

            GLES20.glDeleteProgram(shaderProgramHandle);
            shaderProgramHandle = -1;

        }

        return shaderProgramHandle;
    }

    /**
     * <p>Loads & compiles shader from source</p>
     *
     * @param pIsVertexShader Pass true if shader is vertex shader (false if fragment)
     * @param pSource         Shader source code
     * @return shader handle or 0 if shader compilation failed
     */
    private static int loadShader(final boolean pIsVertexShader, final String pSource) {

        int shader = GLES20.glCreateShader(pIsVertexShader ? GLES20.GL_VERTEX_SHADER : GLES20.GL_FRAGMENT_SHADER);

        if (shader != 0) {

            GLES20.glShaderSource(shader, pSource);
            GLES20.glCompileShader(shader);

            final int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {

                Timber.tag(TAG);
                Timber.e("Could not compile shader:\n%s \n=>%s shader info log:", pSource, pIsVertexShader ? "vertex" : "fragment");
                Timber.e(GLES20.glGetShaderInfoLog(shader));

                GLES20.glDeleteShader(shader);
                shader = 0;

            }

        }
        return shader;
    }

    private UtilsGL() {
    }

}