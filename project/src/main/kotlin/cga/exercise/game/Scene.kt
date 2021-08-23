package cga.exercise.game


import cga.exercise.components.camera.TronCamera
import cga.exercise.components.geometry.*
import cga.exercise.components.light.PointLight
import cga.exercise.components.light.SpotLight
import cga.exercise.components.shader.ShaderProgram
import cga.exercise.components.texture.CubemapTexture
import cga.exercise.components.texture.Texture2D
import cga.framework.GLError
import cga.framework.GameWindow
import cga.framework.ModelLoader
import cga.framework.OBJLoader
import org.joml.*
import org.lwjgl.opengl.GL11
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL30.*
import kotlin.math.PI
import kotlin.math.pow
import kotlin.random.Random
import kotlin.system.exitProcess


class Scene(private val window: GameWindow) {
    private val staticShader: ShaderProgram
    private val skyboxShader: ShaderProgram
    private val toonShader: ShaderProgram
    private val negativeShader: ShaderProgram

    private var shaderInUse: ShaderProgram

    private val resGround: OBJLoader.OBJResult = OBJLoader.loadOBJ("project/assets/models/planet.obj")
    private val objMeshGround: OBJLoader.OBJMesh = resGround.objects[0].meshes[0]

    private var groundMesh: Mesh

    /* private var character = ModelLoader.loadModel(
         "project/assets/models/character.obj",
         Math.toRadians(180.0f),
         Math.toRadians(90.0f),
         Math.toRadians(90.0f)
     ) ?: throw IllegalArgumentException("Could not load the model")*/

    private var character: Animation
    private var player = ModelLoader.loadModel(
        "project/assets/character/char_0.obj", Math.toRadians(180.0f),
        Math.toRadians(-90.0f),
        Math.toRadians(90.0f)
    )


    private var planet = Renderable()

    private var tCamera = TronCamera()

    private var turnedCamera = false

    // Define lights
    private var light = PointLight(Vector3f(), Vector3f())
    private var spotlight = SpotLight(Vector3f(), Vector3f())

    private var collectedAllStars = false
    private var pressedEnter = false
    private var cameraRotationSpeed = 0.0f

    private var oldMousePosX: Double = -1.0
    private var oldMousePosY: Double = -1.0
    private var einbool: Boolean = false

    // Define Vertices and Indices of Cubemap
    private var size: Float = 500.0f
    private var skyboxVertices: FloatArray = floatArrayOf(
        -size, -size, size,
        size, -size, size,
        size, -size, -size,
        -size, -size, -size,
        -size, size, size,
        size, size, size,
        size, size, -size,
        -size, size, -size
    )

    private var skyboxIndices: IntArray = intArrayOf(
        //right
        1, 2, 6,
        6, 5, 1,
        //left
        0, 4, 7,
        7, 3, 0,
        //top
        4, 5, 6,
        6, 7, 4,
        //bottom
        0, 3, 2,
        2, 1, 0,
        //back
        0, 1, 5,
        5, 4, 0,
        //front
        3, 7, 6,
        6, 2, 3
    )

    private var cubeMap = CubemapTexture(skyboxVertices, skyboxIndices)
    private var cubeMapTexture = glGenTextures()


    private var direction = 0.0f
    private var directionHorizontal = 0.0f

    private var difference: Vector3f = Vector3f(0.0f, 0.0f, 0.0f)

    // Collectable list
    private var collectables: MutableList<Star>
    private val collectableAmount: Int = 2
    private var score: Int = 0

    private var finalStar: Star

    // Background Objects
    private var saturnRend: Renderable
    private var neptuneRend: Renderable
    private var earthRend: Renderable
    private var ufoRend: Renderable

    // Jumpingvaria
    private var jumpSpeed = 0f
    private var jumpDirection = false // False = going up, True = going down
    private var canJump = true


    // Scene setup
    init {
        staticShader = ShaderProgram("project/assets/shaders/tron_vert.glsl", "project/assets/shaders/tron_frag.glsl")
        skyboxShader = ShaderProgram("project/assets/shaders/skyBoxVert.glsl", "project/assets/shaders/skyBoxFrag.glsl")
        toonShader = ShaderProgram("project/assets/shaders/toon_vert.glsl", "project/assets/shaders/toon_frag.glsl")
        negativeShader =
            ShaderProgram("project/assets/shaders/tron_vert.glsl", "project/assets/shaders/negative_frag.glsl")

        // Default to static shader
        shaderInUse = staticShader

        glClearColor(0.0f, 0.0f, 0.0f, 1.0f); GLError.checkThrow()
        glDisable(GL_CULL_FACE); GLError.checkThrow()
        //glEnable(GL_CULL_FACE)
        glFrontFace(GL_CCW)
        glCullFace(GL_BACK)
        glEnable(GL_DEPTH_TEST); GLError.checkThrow()
        glDepthFunc(GL_LESS); GLError.checkThrow()

        //-------------------------------------CubeMap--------------------------------------------

        // Loading Cubemap faces
        val facesCubeMap: ArrayList<String> = arrayListOf()
        facesCubeMap.addAll(
            listOf(
                "project/assets/textures/nz.png",
                "project/assets/textures/pz.png",
                "project/assets/textures/py.png",
                "project/assets/textures/ny.png",
                "project/assets/textures/px.png",
                "project/assets/textures/nx.png"
            )
        )

        cubeMapTexture = cubeMap.loadCubeMap(facesCubeMap)

        // --------------------------------Creating Vertex Attributes---------------------------------
        val stride = 8 * 4
        val attrPos = VertexAttribute(3, GL_FLOAT, stride, 0)
        val attrTC = VertexAttribute(2, GL_FLOAT, stride, 3 * 4)
        val attrNorm = VertexAttribute(3, GL_FLOAT, stride, 5 * 4)

        val objVertexAttributes = arrayOf(attrPos, attrTC, attrNorm)

        //-------------------------------------Material------------------------------------------------
        val emitTex = Texture2D("project/assets/textures/4k_venus_atmosphere.jpg", true)
        val diffTex = Texture2D("project/assets/textures/4k_venus_atmosphere.jpg", true)
        val specTex = Texture2D("project/assets/textures/4k_venus_atmosphere.jpg", true)

        val groundMaterial = Material(diffTex, emitTex, specTex, 10.0f, Vector2f(1.0f))

        emitTex.setTexParams(
            GL_REPEAT,
            GL_REPEAT,
            GL11.GL_LINEAR_MIPMAP_LINEAR,
            GL_LINEAR
        )     //Linear = zwischen farbwerten interpolieren
        diffTex.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        specTex.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        groundMesh = Mesh(objMeshGround.vertexData, objMeshGround.indexData, objVertexAttributes, groundMaterial)

        planet.meshList.add(groundMesh)
        planet.scaleLocal(Vector3f(5.0f))

        /*player = ModelLoader.loadModel("project/assets/character/char_0.obj", Math.toRadians(0f), Math.toRadians(180f), 0f)
        if (player == null) {
            exitProcess(1)
        }*/
        //player?.meshList?.get(2)?.material?.emit = Vector3f(1f, 0f, 0f)
        player?.scaleLocal(Vector3f(0.02f))
        player?.setPosition(
            planet.getWorldPosition().x + 5.1f,
            planet.getWorldPosition().y,
            planet.getWorldPosition().z
        )

        character = Animation("project/assets/character/char_", 0, 19, 0f, 180f, 0f)
        character.setParent(player!!)

        character.rotateLocal(
            Math.toRadians(-90.0f),
            Math.toRadians(180.0f),
            Math.toRadians(90.0f)
        )

        // character.scaleLocal(Vector3f(0.3f))


        /* character.setPosition(
             planet.getWorldPosition().x + 5.1f,
             planet.getWorldPosition().y,
             planet.getWorldPosition().z
         )*/
        /*character.translateLocal(
            Vector3f(planet.getWorldPosition().x + 5.1f,
                planet.getWorldPosition().y,
                planet.getWorldPosition().z))*/

        tCamera.parent = player

        tCamera.rotateLocal(Math.toRadians(90.0f), Math.toRadians(45.0f), Math.toRadians(-90.0f))
        tCamera.translateLocal(Vector3f(0.0f, 0.5f, 15.0f))


        //----------------------------------------Light------------------------------------------------
        light = PointLight(tCamera.getWorldPosition(), Vector3f(1.0f))
        light.translateLocal(Vector3f(1.0f, -5.0f, 0.0f))

        light.parent = player


        // Spotlight mit Neigung in x und z Richtung
        spotlight = SpotLight(Vector3f(0.0f, 0.0f, 0.0f), Vector3f(0.0f))
        spotlight.rotateLocal(Math.toRadians(-90.0f), Math.PI.toFloat(), 0.0f)
        spotlight.parent = player
        spotlight.rotateLocal(Math.toRadians(-10.0f), Math.PI.toFloat(), 0.0f)

        //-----------------------------------Collectables-------------------------------------------

        val starEmit = Texture2D("project/assets/textures/StarColor3.png", true)
        val starDiff = Texture2D("project/assets/textures/StarColor3.png", true)
        val starSpec = Texture2D("project/assets/textures/StarColor3.png", true)

        starEmit.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        starDiff.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        starSpec.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        val starMaterial = Material(starDiff, starEmit, starSpec, 40.0f, Vector2f(1.0f))


        collectables = mutableListOf()
        for (i in 0..collectableAmount) {
            val resStar: OBJLoader.OBJResult = OBJLoader.loadOBJ("project/assets/models/Star2.obj")
            val objStar: OBJLoader.OBJMesh = resStar.objects[0].meshes[0]

            val starMesh = Mesh(objStar.vertexData, objStar.indexData, objVertexAttributes, starMaterial)
            val starRend = Renderable()

            starRend.meshList.add(starMesh)

            starRend.scaleLocal(Vector3f(0.1f))

            val starLight = PointLight(starRend.getWorldPosition(), Vector3f(0.6f))
            starLight.parent = starRend


            val star = Star(starLight, starRend, starMaterial)

            star.setPosition(
                planet.getWorldPosition().x + 5.1f,
                planet.getWorldPosition().y,
                planet.getWorldPosition().z
            )

            val randomX = Random.nextFloat() * 360f
            val randomY = Random.nextFloat() * 360f

            star.rotateAroundPoint(0.0f, (i + 1) * PI.toFloat(), (i + 1).toFloat(), planet.getWorldPosition())

            collectables.add(star)
        }

        // Final star appears once all others are collected
        val resFinalStar: OBJLoader.OBJResult = OBJLoader.loadOBJ("project/assets/models/Star2.obj")
        val objFinalStar: OBJLoader.OBJMesh = resFinalStar.objects[0].meshes[0]
        val finalStarMesh = Mesh(objFinalStar.vertexData, objFinalStar.indexData, objVertexAttributes, starMaterial)
        val finalStarRend = Renderable()
        finalStarRend.scaleLocal(Vector3f(0.3f))
        finalStarRend.rotateLocal(0.0f, 0.9f, 0.0f)
        finalStarRend.meshList.add(finalStarMesh)
        val finalStarLight = PointLight(finalStarRend.getWorldPosition(), Vector3f(1f))
        finalStarLight.parent = finalStarRend
        finalStarLight.translateLocal(Vector3f(1.0f, -1.0f, 1.0f))
        finalStar = Star(finalStarLight, finalStarRend, starMaterial)
        finalStar.setPosition(
            planet.getWorldPosition().x + 6f,
            planet.getWorldPosition().y,
            planet.getWorldPosition().z
        )
        //val randomX = Random.nextFloat() * 360f
        //val randomY = Random.nextFloat() * 360f
        finalStar.rotateAroundPoint(0.0f, 50.0f, -30.0f, planet.getWorldPosition())


        //-----------------------Background Objects--------------------------------------------------
        val resSaturn: OBJLoader.OBJResult = OBJLoader.loadOBJ("project/assets/models/saturn2.obj")
        val objMeshSaturn: OBJLoader.OBJMesh = resSaturn.objects[0].meshes[0]

        val saturnEmit = Texture2D("project/assets/textures/2k_saturn.jpg", true)
        val saturnDiff = Texture2D("project/assets/textures/2k_saturn.jpg", true)
        val saturnSpec = Texture2D("project/assets/textures/2k_saturn.jpg", true)

        val saturnMaterial = Material(saturnDiff, saturnEmit, saturnSpec, 10.0f, Vector2f(1.0f))

        saturnEmit.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        saturnDiff.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        saturnSpec.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        val saturnMesh = Mesh(objMeshSaturn.vertexData, objMeshSaturn.indexData, objVertexAttributes, saturnMaterial)

        saturnRend = Renderable()
        saturnRend.meshList.add(saturnMesh)
        saturnRend.scaleLocal(Vector3f(2.0f))
        saturnRend.translateGlobal(Vector3f(0.0f, 15.0f, 10.0f))
        saturnRend.rotateLocal(0.0f, 0.0f, 2.0f)


        val resNeptune: OBJLoader.OBJResult = OBJLoader.loadOBJ("project/assets/models/planet.obj")
        val objMeshNeptune: OBJLoader.OBJMesh = resNeptune.objects[0].meshes[0]

        val neptuneEmit = Texture2D("project/assets/textures/2k_neptune.jpg", true)
        val neptuneDiff = Texture2D("project/assets/textures/2k_neptune.jpg", true)
        val neptuneSpec = Texture2D("project/assets/textures/2k_neptune.jpg", true)

        val neptuneMaterial = Material(neptuneDiff, neptuneEmit, neptuneSpec, 10.0f, Vector2f(1.0f))

        neptuneEmit.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        neptuneDiff.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        neptuneSpec.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        val neptuneMesh =
            Mesh(objMeshNeptune.vertexData, objMeshNeptune.indexData, objVertexAttributes, neptuneMaterial)

        neptuneRend = Renderable()
        neptuneRend.meshList.add(neptuneMesh)
        neptuneRend.scaleLocal(Vector3f(2.0f))
        neptuneRend.translateGlobal(Vector3f(0.0f, -10.0f, -8.0f))
        neptuneRend.rotateLocal(0.0f, 0.0f, 2.0f)


        val resEarth: OBJLoader.OBJResult = OBJLoader.loadOBJ("project/assets/models/planet.obj")
        val objMeshEarth: OBJLoader.OBJMesh = resEarth.objects[0].meshes[0]

        val earthEmit = Texture2D("project/assets/textures/2k_earth_daymap.jpg", true)
        val earthDiff = Texture2D("project/assets/textures/2k_earth_daymap.jpg", true)
        val earthSpec = Texture2D("project/assets/textures/2k_earth_daymap.jpg", true)

        val earthMaterial = Material(earthDiff, earthEmit, earthSpec, 10.0f, Vector2f(1.0f))

        earthEmit.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        earthDiff.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        earthSpec.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        val earthMesh = Mesh(objMeshEarth.vertexData, objMeshEarth.indexData, objVertexAttributes, earthMaterial)

        earthRend = Renderable()
        earthRend.meshList.add(earthMesh)
        earthRend.scaleLocal(Vector3f(2.0f))
        earthRend.translateGlobal(Vector3f(-14.0f, -5.0f, 6.0f))


        val resUfo: OBJLoader.OBJResult = OBJLoader.loadOBJ("project/assets/models/ufo.obj")
        val objMeshUfo: OBJLoader.OBJMesh = resUfo.objects[0].meshes[0]

        val ufoEmit = Texture2D("project/assets/textures/ufo.jpg", true)
        val ufoDiff = Texture2D("project/assets/textures/ufo.jpg", true)
        val ufoSpec = Texture2D("project/assets/textures/ufo.jpg", true)

        val ufoMaterial = Material(ufoDiff, ufoEmit, ufoSpec, 10.0f, Vector2f(1.0f))

        ufoEmit.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        ufoDiff.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        ufoSpec.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        val ufoMesh = Mesh(objMeshUfo.vertexData, objMeshUfo.indexData, objVertexAttributes, ufoMaterial)

        ufoRend = Renderable()
        ufoRend.meshList.add(ufoMesh)
        ufoRend.scaleLocal(Vector3f(0.5f))
        ufoRend.translateGlobal(Vector3f(14.0f, -6.0f, -12.0f))
        ufoRend.rotateLocal(0.0f, 0.0f, 3.0f)


        // shadow mapping
        /*
        val depthMapFrameBuffer = glGenFramebuffers()
        val shadowWidth = 1024
        val shadowHeight = 1024
        val depthMap : Int = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, depthMap)
        GL11.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, shadowWidth, shadowHeight, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
        glViewport(0, 0, shadowWidth, shadowHeight)
        glBindFramebuffer(GL_FRAMEBUFFER, depthMapFrameBuffer)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthMap, 0)
        glDrawBuffer(GL_NONE)
        glReadBuffer(GL_NONE)
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
        glClear(GL_DEPTH_BUFFER_BIT)
        val near_plane = 1.0f.toDouble()
        val far_plane = 7.5f.toDouble()
        val lightProjection = glOrtho(-10.0f.toDouble(), 10.0f.toDouble(), -10.0f.toDouble(), 10.0f.toDouble(), near_plane, far_plane)
        val lightView = Matrix4f().lookAt(Vector3f(-2.0f, 4.0f, -1.0f),Vector3f( 0.0f, 0.0f,  0.0f), Vector3f( 0.0f, 1.0f,  0.0f))
        //val lightSpaceMatrix : Matrix4f = lightView.mul(lightProjection)

         */
    }


    fun render(dt: Float, t: Float) {

        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        // -----------------------rendering Skybox----------------------------
        glDepthFunc(GL_LEQUAL)
        skyboxShader.use()

        skyboxShader.setUniform("view", tCamera.getCalculateViewMatrix(), false)
        skyboxShader.setUniform("projection", tCamera.getCalculateProjectionMatrix(), false)

        glBindVertexArray(cubeMap.skyboxVAO)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_CUBE_MAP, cubeMapTexture)
        glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0)

        glBindVertexArray(0);
        glDepthFunc(GL_LESS);

        //-----------------rendering Camera, Light, Character and Planet-----------------------

        shaderInUse.use()
        tCamera.bind(shaderInUse)
        shaderInUse.setUniform("farbe", Vector3f(1.0f))
        light.bind(shaderInUse, "Point")
        spotlight.bind(shaderInUse, "Spot", tCamera.getCalculateViewMatrix())
        shaderInUse.setUniform("farbe", Vector3f(0.0f, 0.0f, 0.0f))
        planet.render(shaderInUse)
        character.render(shaderInUse, dt)


        //----------------------rendering Background Objects-----------------------------------
        shaderInUse.use()
        shaderInUse.setUniform("farbe", Vector3f(0.5f))
        saturnRend.render(shaderInUse)
        neptuneRend.render(shaderInUse)
        earthRend.render(shaderInUse)
        ufoRend.render(shaderInUse)


        //------------------rendering Collectables---------------------------------------------
        for (i in 0 until collectableAmount) {
            collectables[i].render(shaderInUse, "Point")
        }

        if (score >= collectableAmount) {
            finalStar.render(shaderInUse, "Point")
        }
    }

    fun lengthdir_x(length: Float, dir: Float): Float =
        (length * (Math.cos(dir.toDouble()))).toFloat() // Calculates point based on Direction and length - X

    fun lengthdir_y(length: Float, dir: Float): Float =
        (length * (Math.sin(dir.toDouble()))).toFloat() // Calculates point based on Direction and length - X

    fun lengthdir_z(length: Float, dir: Float): Float =
        (length * (-Math.sin(dir.toDouble()))).toFloat() // Calculates point based on Direction and length - Y

    fun update(dt: Float, t: Float) {
        //-----------------------Player Movement on planet--------------------------------------

        character.update()

        // Character is not moving
        if (!window.getKeyState(GLFW_KEY_S) && !window.getKeyState(GLFW_KEY_W)) {
            character.movement = false
        }

        if (collectedAllStars) {
            if (window.getKeyState(GLFW_KEY_ENTER)) {
                tCamera.setPosition(player!!.x(), player!!.y(), player!!.z())
                collectedAllStars = false
                pressedEnter = true
            }
        } else {
            if (jumpSpeed == 0f) {
                if (window.getKeyState(GLFW_KEY_W)) {
                    character.movement = true
                    direction -= 0.2f
                    player!!.setPosition(
                        lengthdir_x(5.1f, Math.toRadians(direction)),
                        lengthdir_z(5.1f, Math.toRadians(direction)),
                        lengthdir_z(5.1f, 0.0f)
                    )
                    player!!.rotateLocal(0.0f, 0.0f, Math.toRadians(0.2f))
                } else if (window.getKeyState(GLFW_KEY_S)) {
                    character.movement = true
                    direction += 0.2f
                    player!!.setPosition(
                        lengthdir_x(5.1f, Math.toRadians(direction)),
                        lengthdir_z(5.1f, Math.toRadians(direction)),
                        lengthdir_z(5.1f, 0.0f)
                    )
                    player!!.rotateLocal(0.0f, 0.0f, Math.toRadians(-0.2f))
                }
                if (!turnedCamera) {
                    if (window.getKeyState(GLFW_KEY_F1)) {
                        tCamera.translateLocal(Vector3f(20.0f, 10f, -10.0f))
                        tCamera.rotateLocal(Math.toRadians(-45.0f), Math.toRadians(45.0f), Math.toRadians(90.0f))
                        turnedCamera = true
                    }
                } else {
                    if (window.getKeyState(GLFW_KEY_F2)) {
                        tCamera.rotateLocalBack(Math.toRadians(45.0f), Math.toRadians(-45.0f), Math.toRadians(-90.0f))
                        tCamera.translateLocal(Vector3f(20.0f, 10f, -10.0f).negate())
                        turnedCamera = false
                    }
                }


            }
        }


        //---------------------------Jump Handling------------------------------------------------

        // Player is on the ground
        if (checkCollisionWithPlanet()) {
            jumpSpeed = 0f
            canJump = true
            jumpDirection = false
        }

        // Player is on the ground and presses space
        if (window.getKeyState(GLFW_KEY_SPACE) && canJump) {
            canJump = false
            character.movement = false
            jumpSpeed = -0.015f
        }

        // Player is airborne
        if (!canJump) {
            jumpSpeed += (if (jumpDirection) -1 else 1) * 0.0005f


            // Calculate jumping vector
            var jumpingVector = Vector3f(
                planet.x() - player!!.x(),
                planet.y() - player!!.y(),
                planet.z() - player!!.z()
            )

            jumpingVector = jumpingVector.mul(jumpSpeed)

            val oldCharacterPosition = player!!.getWorldPosition()
            val newCharacterPosition = oldCharacterPosition.add(jumpingVector)
            player!!.setPosition(newCharacterPosition.x(), newCharacterPosition.y(), newCharacterPosition.z())

            if (jumpSpeed > 0.015) {
                jumpDirection = true
            }

        }

        //------------------Animate stars & check for player collision-------------------------------
        for (star in collectables) {
            if (star.distance(player!!) < 0.2f) {
                if (star.collect()) {
                    score++
                }
            }
            star.rotate(dt)
        }

        if (score >= collectableAmount && !pressedEnter && !collectedAllStars) {
            collectedAllStars = true
            //tCamera.setPosition(tCamera.getWorldPosition().x, tCamera.getWorldPosition().y + cameraRotationSpeed, tCamera.getWorldPosition().z)

            //cameraRotationSpeed += 0.01f

            //if(cameraRotationSpeed > 0.05) {
            //    cameraRotationSpeed = 0.05f
            //}

            //tCamera.rotateLocal(Math.toRadians(90.0f), Math.toRadians(45.0f), Math.toRadians(-90.0f))

        }

        /*if (collectedAllStars) {
            // cameraRotationSpeed += 0.01f

            if (difference.x == 0.0f && difference.y == 0.0f && difference.z == 0.0f) {
                difference = Vector3f(
                    finalStar.x() - tCamera.getWorldPosition().x,
                    finalStar.y() - tCamera.getWorldPosition().y,
                    finalStar.z() - tCamera.getWorldPosition().z
                )
            }

            var direction = Vector3f(
                finalStar.getXDir().toFloat() - tCamera.getXDir().toFloat(),
                finalStar.getYDir().toFloat() - tCamera.getYDir().toFloat(),
                finalStar.getZDir().toFloat() - tCamera.getZDir().toFloat()
            )


          //  println(tCamera.getWorldPosition() != finalStar.getPosition())
            if (pointDistance3d(tCamera.getWorldPosition().x, tCamera.getWorldPosition().y, tCamera.getWorldPosition().z, finalStar.getPosition().x, finalStar.getPosition().y, finalStar.getPosition().z) > 0.5) {
                println(tCamera.getWorldPosition())
                tCamera.setPosition(

                        tCamera.getWorldPosition().x + difference.x / 10.0f,
                    tCamera.getWorldPosition().y + difference.y / 10.0f,
                    tCamera.getWorldPosition().z + difference.z / 10.0f

                )

            } else {
                println("SIND DA")
            }

        }*/


        if (finalStar.distance(player!!) < 0.3f && score >= collectableAmount) {
            if (finalStar.collect()) {
                println("Du hast gewonnen! Man bist du krass!")

            }
        }

        //--------------------Movement of Background Objects-------------------------
        ufoRend.rotateAroundPoint(dt / 20, 0.0f, 0.0f, planet.getWorldPosition())
        ufoRend.rotateLocal(0.0f, dt * 5, 0.0f)
        earthRend.rotateAroundPoint(dt / 20, 0.0f, 0.0f, planet.getWorldPosition())
        neptuneRend.rotateAroundPoint(dt / 20, 0.0f, 0.0f, planet.getWorldPosition())
        saturnRend.rotateAroundPoint(dt / 20, 0.0f, 0.0f, planet.getWorldPosition())


        //---------------------Handle shader switching--------------------------------
        if (window.getKeyState(GLFW_KEY_1)) {
            shaderInUse = staticShader
        }
        if (window.getKeyState(GLFW_KEY_2)) {
            shaderInUse = toonShader
        }
        if (window.getKeyState(GLFW_KEY_3)) {
            shaderInUse = negativeShader
        }
    }

    fun pointDistance3d(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float =
        Math.sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2) + (z2 - z1).pow(2))

    fun checkCollisionWithPlanet(): Boolean {
        // val collisionMin = 5.1f
        val collisionMax = 5.101f
        val currentDifference =
            pointDistance3d(planet.x(), planet.y(), planet.z(), player!!.x(), player!!.y() + jumpSpeed, player!!.z())
        if (currentDifference <= collisionMax) {
            return true
        }
        return false
    }

    fun onKey(key: Int, scancode: Int, action: Int, mode: Int) {}

    fun onMouseMove(xpos: Double, ypos: Double) {
        /*//Bewegung in x Richtung durch Differenz zwischen alter und neuer Position
        var deltaX: Double = xpos - oldMousePosX
        var deltaY: Double = ypos - oldMousePosY
        oldMousePosX = xpos
        oldMousePosY = ypos

        if (einbool) {
            tCamera.rotateAroundPoint(Math.toRadians(deltaX.toFloat() * 0.05f), 0.0f, 0.0f, Vector3f(0.0f))
        }
        einbool = true
    */
    }

    fun cleanup() {}
}