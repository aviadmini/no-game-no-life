package com.aviadmini.nogamenolife.opengl;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.support.annotation.NonNull;

import com.aviadmini.nogamenolife.compute.LifeCompute;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class LifeRenderer
        implements GLSurfaceView.Renderer {

    private static final int HANDLE_INVALID = -1;

    private static final String VERTEX_SHADER = //
            "precision mediump float;\n" +
                    "\n" +
                    "attribute vec4 a_vertex_coord;\n" +
                    "attribute vec2 a_texture_coord;\n" +
                    "\n" +
                    "varying vec2 v_texture_coord;\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "    v_texture_coord = a_texture_coord;\n" +
                    "    gl_Position = a_vertex_coord;\n" +
                    "}";


    // For easier processing black is dead, white is alive
    private static final String FRAGMENT_SHADER = //
            "precision mediump float;\n" +
                    "precision lowp sampler2D;\n" +
                    "\n" +
                    "uniform sampler2D t_texture;\n" +
                    "\n" +
                    "uniform vec2 u_chunk_size;\n" +
                    "\n" +
                    "varying vec2 v_texture_coord;\n" +
                    "\n" +
                    "float get(float x, float y) {\n" +
                    "    return texture2D(t_texture, v_texture_coord + vec2(x, y)).r;\n" +
                    "}\n" +
                    "\n" +
                    "void main() {\n" +
                    "    float sum = get(-u_chunk_size.x, -u_chunk_size.y) +\n" +
                    "                get(-u_chunk_size.x, 0.0) +\n" +
                    "                get(-u_chunk_size.x, u_chunk_size.y) +\n" +
                    "                get(0.0, -u_chunk_size.y) +\n" +
                    "                get(0.0,  u_chunk_size.y) +\n" +
                    "                get(u_chunk_size.x, -u_chunk_size.y) +\n" +
                    "                get(u_chunk_size.x, 0.0) +\n" +
                    "                get(u_chunk_size.x, u_chunk_size.y);\n" +
                    "    if (sum == 3.0) {\n" +
                    "        gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);\n" +
                    "    } else if (sum == 2.0) {\n" +
                    "        float current = float(get(0.0, 0.0));\n" +
                    "        gl_FragColor = vec4(current, current, current, 1.0);\n" +
                    "    } else {\n" +
                    "        gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);\n" +
                    "    }\n" +
                    "}";

    private final Callback mCallback;

    private int[] mInitialStates;

    private int mWidth  = 1;
    private int mHeight = 1;

    private float mChunkX = 1.0f;
    private float mChunkY = 1.0f;

    private int mShaderProgramHandle = HANDLE_INVALID;

    private int mTextureOneHandle = HANDLE_INVALID;
    private int mTextureTwoHandle = HANDLE_INVALID;

    private int mFramebufferOneHandle = HANDLE_INVALID;
    private int mFramebufferTwoHandle = HANDLE_INVALID;

    private boolean mUsingOne;

    private int mGLSamplerTextureLocation;
    private int mGLUniformChunkSizeLocation;
    private int mGLAttributeVertexCoordinateLocation;
    private int mGLAttributeTextureCoordinateLocation;

    private final float[] mQuad = UtilsGL.getQuadFbo();
    protected final FloatBuffer mAttributeBuffer;

    private IntBuffer mPixelsIntBuffer;
    private IntBuffer mOnePixelIntBuffer;

    private int[] mCellStatesGL;
    private int[] mConvertedCellStates;

    public LifeRenderer(final Callback pCallback) {
        this(pCallback, null);
    }

    public LifeRenderer(final Callback pCallback, final int[] pInitialStates) {

        this.mCallback = pCallback;

        this.mAttributeBuffer = ByteBuffer.allocateDirect(this.mQuad.length * UtilsGL.BYTES_PER_FLOAT)
                                          .order(ByteOrder.nativeOrder())
                                          .asFloatBuffer();
        this.mAttributeBuffer.put(this.mQuad)
                             .position(0);


        if (pInitialStates != null) {
            this.mInitialStates = pInitialStates;
        }

    }

    @Override
    public void onSurfaceCreated(final GL10 pGL10Unused, final EGLConfig pEGLConfig) {

        this.clearBlack();

        final ByteBuffer onePixelByteBuffer = ByteBuffer.allocateDirect(UtilsGL.BYTES_PER_FLOAT);
        onePixelByteBuffer.order(ByteOrder.nativeOrder());
        this.mOnePixelIntBuffer = onePixelByteBuffer.asIntBuffer();

        this.createShaderProgram();

        this.createAttributesAndUniforms();

        UtilsGL.logErrorGL("onSurfaceCreated end");

    }

    @Override
    public void onSurfaceChanged(final GL10 pGL10Unused, final int pWidth, final int pHeight) {

        this.mWidth = pWidth;
        this.mHeight = pHeight;

        this.mChunkX = 1.0f / pWidth;
        this.mChunkY = 1.0f / pHeight;

        final ByteBuffer pixelsByteBuffer = ByteBuffer.allocateDirect(pWidth * pHeight * UtilsGL.BYTES_PER_FLOAT);
        pixelsByteBuffer.order(ByteOrder.nativeOrder());
        this.mPixelsIntBuffer = pixelsByteBuffer.asIntBuffer();

        this.mCellStatesGL = new int[pWidth * pHeight];
        this.mConvertedCellStates = new int[pWidth * pHeight];

        if (this.mInitialStates != null && this.mInitialStates.length == pWidth * pHeight) {
            // Timber.v("initWithCellStates: %s", Arrays.toString(this.mInitialStates));
            this.initWithCellStates(this.mInitialStates);
        } else {
            //            Timber.v("init blank");

            for (int i = this.mCellStatesGL.length - 1; i >= 0; i--) {

                this.mCellStatesGL[i] = Color.BLACK;
                this.convertCellStates();

            }

        }

        this.mPixelsIntBuffer.rewind();
        this.mPixelsIntBuffer.put(this.mCellStatesGL);

        GLES20.glViewport(0, 0, pWidth, pHeight);

        this.createTextures();

        this.createFramebuffers();

        this.applyInitialStates();

        UtilsGL.logErrorGL("onSurfaceChanged end");

    }

    @Override
    public void onDrawFrame(final GL10 pGL10Unused) {

        // framebuffer bind

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, this.getToFBO());

        // shader program

        GLES20.glUseProgram(this.mShaderProgramHandle);

        // texture

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, this.getFromTexture());

        GLES20.glUniform1i(this.mGLSamplerTextureLocation, 0);

        // attributes

        this.activateAttributes();

        this.mAttributeBuffer.position(0);
        GLES20.glVertexAttribPointer(this.mGLAttributeVertexCoordinateLocation, 2, GLES20.GL_FLOAT, false, 4 * UtilsGL.BYTES_PER_FLOAT,
                this.mAttributeBuffer);
        this.mAttributeBuffer.position(2);
        GLES20.glVertexAttribPointer(this.mGLAttributeTextureCoordinateLocation, 2, GLES20.GL_FLOAT, false, 4 * UtilsGL.BYTES_PER_FLOAT,
                this.mAttributeBuffer);

        // uniforms

        GLES20.glUniform2f(this.mGLUniformChunkSizeLocation, this.mChunkX, this.mChunkY);

        // draw call

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, this.mQuad.length / 4);

        // swap from & to FBOs

        this.mUsingOne = !this.mUsingOne;

        // deactivate attributes

        this.deactivateAttributes();

        // framebuffer unbind

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        // callback

        if (this.mCallback != null) {
            this.mCallback.onFrameDrawn();
        }

        UtilsGL.logErrorGL("onDrawFrame end");

    }

    private void createShaderProgram() {
        this.mShaderProgramHandle = UtilsGL.createShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER);
    }

    private void createAttributesAndUniforms() {

        this.mGLAttributeVertexCoordinateLocation = GLES20.glGetAttribLocation(this.mShaderProgramHandle, "a_vertex_coord");
        this.mGLAttributeTextureCoordinateLocation = GLES20.glGetAttribLocation(this.mShaderProgramHandle, "a_texture_coord");

        this.mGLUniformChunkSizeLocation = GLES20.glGetUniformLocation(this.mShaderProgramHandle, "u_chunk_size");

        this.mGLSamplerTextureLocation = GLES20.glGetUniformLocation(this.mShaderProgramHandle, "t_texture");

    }

    private void activateAttributes() {

        GLES20.glEnableVertexAttribArray(this.mGLAttributeVertexCoordinateLocation);
        GLES20.glEnableVertexAttribArray(this.mGLAttributeTextureCoordinateLocation);

    }

    private void deactivateAttributes() {

        GLES20.glDisableVertexAttribArray(this.mGLAttributeVertexCoordinateLocation);
        GLES20.glDisableVertexAttribArray(this.mGLAttributeTextureCoordinateLocation);

    }

    private void createTextures() {

        this.destroyTextures();

        final int[] ids = new int[2];

        GLES20.glGenTextures(2, ids, 0);

        //        Timber.v("tx: " + Arrays.toString(ids));

        this.mTextureOneHandle = ids[0];
        this.mTextureTwoHandle = ids[1];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, this.mTextureOneHandle);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, this.mWidth, this.mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                null);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, this.mTextureTwoHandle);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, this.mWidth, this.mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                null);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

    }

    private void createFramebuffers() {

        this.destroyFramebuffers();

        final int[] ids = new int[2];

        GLES20.glGenFramebuffers(2, ids, 0);

        //Timber.v( "fbos: " + Arrays.toString(ids));

        this.mFramebufferOneHandle = ids[0];
        this.mFramebufferTwoHandle = ids[1];

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, this.mFramebufferOneHandle);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, this.mTextureOneHandle, 0);

        //        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) == GLES20.GL_FRAMEBUFFER_COMPLETE) {
        //            Timber.v("Framebuffer OK: " + this.mFramebufferOneHandle);
        //        } else {
        //            Timber.v("Framebuffer check produced error: " + this.mFramebufferOneHandle);
        //        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, this.mFramebufferTwoHandle);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, this.mTextureTwoHandle, 0);

        //        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) == GLES20.GL_FRAMEBUFFER_COMPLETE) {
        //            Timber.v("Framebuffer OK: " + this.mFramebufferTwoHandle);
        //        } else {
        //            Timber.v("Framebuffer check produced error: " + this.mFramebufferTwoHandle);
        //        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        this.mUsingOne = true;

    }

    private void applyInitialStates() {

        this.mPixelsIntBuffer.rewind();

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, this.getFromTexture());

        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, this.mWidth, this.mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                this.mPixelsIntBuffer);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

    }

    private void destroyTextures() {

        final int[] ids = new int[1];

        if (this.mTextureOneHandle != HANDLE_INVALID) {

            ids[0] = this.mTextureOneHandle;
            GLES20.glDeleteTextures(1, ids, 0);

            this.mTextureOneHandle = HANDLE_INVALID;

        }

        if (this.mTextureTwoHandle != HANDLE_INVALID) {

            ids[0] = this.mTextureTwoHandle;
            GLES20.glDeleteTextures(1, ids, 0);

            this.mTextureTwoHandle = HANDLE_INVALID;

        }

    }

    private void destroyFramebuffers() {

        final int[] ids = new int[1];

        if (this.mFramebufferOneHandle != HANDLE_INVALID) {

            ids[0] = this.mFramebufferOneHandle;
            GLES20.glDeleteFramebuffers(1, ids, 0);

            this.mFramebufferOneHandle = HANDLE_INVALID;

        }

        if (this.mFramebufferTwoHandle != HANDLE_INVALID) {

            ids[0] = this.mFramebufferTwoHandle;
            GLES20.glDeleteFramebuffers(1, ids, 0);

            this.mFramebufferTwoHandle = HANDLE_INVALID;

        }

    }

    private int getFromFBO() {
        return this.mUsingOne ? this.mFramebufferOneHandle : this.mFramebufferTwoHandle;
    }

    private int getToFBO() {
        return this.mUsingOne ? this.mFramebufferTwoHandle : this.mFramebufferOneHandle;
    }

    private int getFromTexture() {
        return this.mUsingOne ? this.mTextureOneHandle : this.mTextureTwoHandle;
    }

    public synchronized void changeCellState(final int pCellPosition, final int pNewState) {

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        final int newStateGL = pNewState == LifeCompute.STATE_ALIVE ? Color.WHITE : Color.BLACK;
        this.mOnePixelIntBuffer.put(0, newStateGL);

        this.mOnePixelIntBuffer.rewind();

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, this.getFromTexture());

        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, pCellPosition % this.mWidth, pCellPosition / this.mWidth, 1, 1, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, this.mOnePixelIntBuffer);

        //        Timber.v("pos = [" + pCellPosition % this.mWidth + ", " + pCellPosition / this.mWidth + "]");

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        this.mCellStatesGL[pCellPosition] = newStateGL;
        this.mConvertedCellStates[pCellPosition] = pNewState;

    }

    public synchronized int getCellState(final int pCellPosition) {

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, this.getFromFBO());

        this.mOnePixelIntBuffer.rewind();

        GLES20.glReadPixels(pCellPosition % this.mWidth, pCellPosition / this.mWidth, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                this.mOnePixelIntBuffer);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        this.mCellStatesGL[pCellPosition] = this.mOnePixelIntBuffer.get(0);
        this.mConvertedCellStates[pCellPosition] =
                this.mCellStatesGL[pCellPosition] == Color.WHITE ? LifeCompute.STATE_ALIVE : LifeCompute.STATE_DEAD;

        //        Timber.v("cell pos = [%s, %s], value = %s", pCellPosition % this.mWidth, pCellPosition / this.mWidth, this.mCellStatesGL[pCellPosition]);

        return this.mConvertedCellStates[pCellPosition];
    }

    public synchronized void clear() {

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, this.mFramebufferOneHandle);
        this.clearBlack();

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, this.mFramebufferTwoHandle);
        this.clearBlack();

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

    }

    private void clearBlack() {

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

    }

    @NonNull
    public int[] getCellStates() {

        this.mPixelsIntBuffer.rewind();

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, this.getFromFBO());

        GLES20.glReadPixels(0, 0, this.mWidth, this.mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, this.mPixelsIntBuffer);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        this.mPixelsIntBuffer.rewind();
        this.mPixelsIntBuffer.get(this.mCellStatesGL);

        this.convertCellStates();

        //Timber.v( "1. gl result: " + Arrays.toString(this.mCellStatesGL));
        //Timber.v( "1. converted result: " + Arrays.toString(this.mConvertedCellStates));

        return this.mConvertedCellStates;
    }

    private void convertCellStates() {

        for (int i = this.mCellStatesGL.length - 1; i >= 0; i--) {

            if (this.mCellStatesGL[i] == Color.WHITE) {
                this.mConvertedCellStates[i] = LifeCompute.STATE_ALIVE;
            } else {
                this.mConvertedCellStates[i] = LifeCompute.STATE_DEAD;
            }

        }

    }

    private void initWithCellStates(final int[] pCellStates) {

        this.mConvertedCellStates = pCellStates.clone();

        for (int i = this.mConvertedCellStates.length - 1; i >= 0; i--) {

            if (this.mConvertedCellStates[i] == LifeCompute.STATE_ALIVE) {
                this.mCellStatesGL[i] = Color.WHITE;
            } else {
                this.mCellStatesGL[i] = Color.BLACK;
            }

        }

    }

    public interface Callback {
        void onFrameDrawn();
    }

}