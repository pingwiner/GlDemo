package com.example.glanimation;

import android.content.Context;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

public class OpenGLRenderer implements Renderer {
    private static OpenGLRenderer instance;

    private final static int POSITION_COUNT = 3;
    private static final int TEXTURE_COUNT = 2;
    private static final int STRIDE = (POSITION_COUNT
            + TEXTURE_COUNT) * 4;

    private Context context;

    private FloatBuffer vertexData;

    private int aPositionLocation;
    private int aTextureLocation;
    private int uTextureUnitLocation;
    private int uMatrixLocation;

    private int programId;

    private float[] mProjectionMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mMatrix = new float[16];
    private float[] mModelMatrix = new float[16];

    private int bgTexture;
    private int smileTexture;

    private float offsetX = -1f;
    private float offsetY = 2.5f;

    private int phase = 0;
    private boolean stopped = true;

    private OpenGLRenderer(Context context) {
        this.context = context;
    }

    public static OpenGLRenderer getInstance(Context context) {
        if (instance == null) {
            instance = new OpenGLRenderer(context);
        } else {
            instance.context = context;
        }
        return instance;
    }

    @Override
    public void onSurfaceCreated(GL10 arg0, EGLConfig arg1) {
        glClearColor(0f, 0f, 0f, 1f);
        glEnable(GL_DEPTH_TEST);

        createAndUseProgram();
        getLocations();
        prepareData();
        bindData();
        createViewMatrix();
    }

    @Override
    public void onSurfaceChanged(GL10 arg0, int width, int height) {
        int left = 0;
        int top = 0;
        if (width > height) {
            int oldWidth = width;
            width = height * 4 / 5;
            left = (oldWidth - width) / 2;
        } else {
            int oldHeight = height;
            height = width * 5 / 4;
            top = (oldHeight - height) / 2;
        }
        glViewport(left, top, width, height);
        createProjectionMatrix(width, height);
        bindMatrix();
    }

    private void prepareData() {

        float[] vertices = {
                //coordinates for background
                -2, 4f, 0,   0, 0,
                -2, -4f, 0,  0, 0.5f,
                2,  4f, 0,   0.5f, 0,
                2, -4f, 0,   0.5f, 0.5f,

                //coordinates for smile
                -0.3f, 0.3f, 0.5f,      0, 1f,
                -0.3f, -0.3f, 0.5f,      0, 0,
                0.3f,  0.3f, 0.5f,      1f, 1f,
                0.3f, -0.3f, 0.5f,       1f, 0,
        };

        vertexData = ByteBuffer
                .allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexData.put(vertices);

        bgTexture = TextureUtils.loadTexture(context, R.drawable.bg);
        smileTexture = TextureUtils.loadTexture(context, R.drawable.smile);
    }

    private void createAndUseProgram() {
        int vertexShaderId = ShaderUtils.createShader(context, GL_VERTEX_SHADER, R.raw.vertex_shader);
        int fragmentShaderId = ShaderUtils.createShader(context, GL_FRAGMENT_SHADER, R.raw.fragment_shader);
        programId = ShaderUtils.createProgram(vertexShaderId, fragmentShaderId);
        glUseProgram(programId);
    }

    private void getLocations() {
        aPositionLocation = glGetAttribLocation(programId, "a_Position");
        aTextureLocation = glGetAttribLocation(programId, "a_Texture");
        uTextureUnitLocation = glGetUniformLocation(programId, "u_TextureUnit");
        uMatrixLocation = glGetUniformLocation(programId, "u_Matrix");
    }

    private void bindData() {
        vertexData.position(0);
        glVertexAttribPointer(aPositionLocation, POSITION_COUNT, GL_FLOAT,
                false, STRIDE, vertexData);
        glEnableVertexAttribArray(aPositionLocation);

        vertexData.position(POSITION_COUNT);
        glVertexAttribPointer(aTextureLocation, TEXTURE_COUNT, GL_FLOAT,
                false, STRIDE, vertexData);
        glEnableVertexAttribArray(aTextureLocation);
    }

    private void createProjectionMatrix(int width, int height) {
        float ratio = 1;
        float left = -0.5f;
        float right = 0.5f;
        float bottom = -0.5f;
        float top = 0.5f;
        float near = 2;
        float far = 12;

        if (width > height) {
            ratio = (float) width / height;
            left *= ratio;
            right *= ratio;
        } else {
            ratio = (float) height / width;
            bottom *= ratio;
            top *= ratio;
        }

        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
    }

    private void createViewMatrix() {
        float eyeX = 0;
        float eyeY = 2f;
        float eyeZ = 7;

        float centerX = 0;
        float centerY = 1;
        float centerZ = 0;

        float upX = 0;
        float upY = 1;
        float upZ = 0;

        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
    }


    private void bindMatrix() {
        Matrix.multiplyMM(mMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMatrix, 0, mProjectionMatrix, 0, mMatrix, 0);
        glUniformMatrix4fv(uMatrixLocation, 1, false, mMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 arg0) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        Matrix.setIdentityM(mModelMatrix, 0);
        bindMatrix();

        glBindTexture(GL_TEXTURE_2D, bgTexture);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        glBindTexture(GL_TEXTURE_2D, smileTexture);
        Matrix.setIdentityM(mModelMatrix, 0);
        setModelMatrix();
        bindMatrix();
        glDrawArrays(GL_TRIANGLE_STRIP, 4, 4);
    }

    private void setModelMatrix() {
        if (!stopped) {
            switch (phase) {
                case 0:
                    offsetX += 0.01f;
                    if (offsetX >= 1f) {
                        phase = 1;
                    }
                    break;
                case 1:
                    offsetX -= 0.01f;
                    offsetY -= 0.015f;
                    if (offsetX <= -1f) {
                        phase = 2;
                    }
                    break;
                case 2:
                    offsetX += 0.01f;
                    if (offsetX >= 1f) {
                        phase = 3;
                    }
                    break;
                default:
            }
        }
        Matrix.translateM(mModelMatrix, 0, offsetX, offsetY, 0);
    }

    public void start() {
        if (phase == 3) {
            offsetX = -1f;
            offsetY = 2.5f;
            phase = 0;
        }
        stopped = false;
    }

    public void stop() {
        stopped = true;
    }

    public void onDestroy() {
        context = null;
    }
}


