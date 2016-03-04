package com.aviadmini.nogamenolife.opengl;

import android.opengl.GLSurfaceView;
import android.support.annotation.NonNull;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * Not every device supports this because of OpenGL extension it uses
 */
public class OffscreenGLContextManager {

    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

    private final int mWidth;
    private final int mHeight;

    private final GLSurfaceView.Renderer mRenderer;

    private EGL10      mEGL;
    private EGLDisplay mEGLDisplay;
    private EGLContext mEGLContext;
    private EGLSurface mEGLSurface;

    private boolean mCreated;

    public OffscreenGLContextManager(@NonNull final GLSurfaceView.Renderer pRenderer, final int pWidth, final int pHeight) {

        this.mRenderer = pRenderer;

        this.mWidth = pWidth;
        this.mHeight = pHeight;

    }

    public void create() {

        if (this.mCreated) {
            return;
        }

        final int[] version = new int[2];

        this.mEGL = (EGL10) EGLContext.getEGL();
        this.mEGLDisplay = this.mEGL.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        this.mEGL.eglInitialize(this.mEGLDisplay, version);

        final EGLConfig eglConfig = this.chooseConfig();

        final int[] eglContextAttributeList = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE};
        this.mEGLContext = this.mEGL.eglCreateContext(this.mEGLDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, eglContextAttributeList);

        final int[] eglSurfaceAttributeList = {EGL10.EGL_WIDTH, this.mWidth, EGL10.EGL_HEIGHT, this.mHeight, EGL10.EGL_NONE};
        this.mEGLSurface = this.mEGL.eglCreatePbufferSurface(this.mEGLDisplay, eglConfig, eglSurfaceAttributeList);

        this.mEGL.eglMakeCurrent(this.mEGLDisplay, this.mEGLSurface, this.mEGLSurface, this.mEGLContext);

        this.mRenderer.onSurfaceCreated(null, eglConfig);
        this.mRenderer.onSurfaceChanged(null, this.mWidth, this.mHeight);

        this.mCreated = true;

    }

    public void render() {
        this.mRenderer.onDrawFrame(null);
    }

    public void destroy() {

        if (!this.mCreated) {
            return;
        }

        this.mEGL.eglMakeCurrent(this.mEGLDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);

        this.mEGL.eglDestroySurface(this.mEGLDisplay, this.mEGLSurface);

        this.mEGL.eglDestroyContext(this.mEGLDisplay, this.mEGLContext);

        this.mEGL.eglTerminate(this.mEGLDisplay);

    }

    private EGLConfig chooseConfig() {

        final int[] attributeList = {EGL10.EGL_DEPTH_SIZE, 16,//
                EGL10.EGL_STENCIL_SIZE, 0,//
                EGL10.EGL_RED_SIZE, 8,//
                EGL10.EGL_GREEN_SIZE, 8,//
                EGL10.EGL_BLUE_SIZE, 8,//
                EGL10.EGL_ALPHA_SIZE, 8,//
                EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,//
                EGL10.EGL_RENDERABLE_TYPE, 4,//
                EGL10.EGL_NONE,//
        };

        final int[] numConfig = new int[1];
        this.mEGL.eglChooseConfig(this.mEGLDisplay, attributeList, null, 0, numConfig);
        final int configSize = numConfig[0];
        final EGLConfig[] eglConfigs = new EGLConfig[configSize];
        this.mEGL.eglChooseConfig(this.mEGLDisplay, attributeList, eglConfigs, configSize, numConfig);

        return eglConfigs[0];
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public boolean isCreated() {
        return this.mCreated;
    }

}
