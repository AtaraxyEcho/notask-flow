<template>
  <div class="login-water-background">
    <div ref="sceneMountRef" class="login-water-canvas" aria-hidden="true"></div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

interface ThreeModule {
  WebGLRenderer: new (options?: Record<string, unknown>) => ThreeRenderer
  Scene: new () => ThreeScene
  OrthographicCamera: new (
    left: number,
    right: number,
    top: number,
    bottom: number,
    near: number,
    far: number
  ) => ThreeCamera
  Clock: new () => ThreeClock
  DataTexture: new (
    data: Float32Array,
    width: number,
    height: number,
    format: number,
    type: number
  ) => ThreeDataTexture
  PlaneGeometry: new (width: number, height: number) => ThreeGeometry
  ShaderMaterial: new (options: Record<string, unknown>) => ThreeShaderMaterial
  Mesh: new (geometry: ThreeGeometry, material: ThreeShaderMaterial) => ThreeMesh
  Vector2: new (x?: number, y?: number) => ThreeVector2
  Vector3: new (x?: number, y?: number, z?: number) => ThreeVector3
  LinearFilter: number
  FloatType: number
  RedFormat?: number
  LuminanceFormat?: number
}

interface ThreeRenderer {
  domElement: HTMLCanvasElement
  setSize: (width: number, height: number) => void
  setPixelRatio: (value: number) => void
  render: (scene: ThreeScene, camera: ThreeCamera) => void
  dispose: () => void
}

interface ThreeScene {
  add: (object: unknown) => void
}

interface ThreeCamera {
  left: number
  right: number
  top: number
  bottom: number
  position: {
    z: number
  }
  updateProjectionMatrix: () => void
}

interface ThreeClock {
  getDelta: () => number
}

interface ThreeDataTexture {
  image: {
    data: Float32Array
  }
  minFilter: number
  magFilter: number
  needsUpdate: boolean
  dispose?: () => void
}

interface ThreeGeometry {
  dispose: () => void
}

interface ThreeShaderMaterial {
  uniforms: {
    rippleStrength: {
      value: number
    }
    resolution: {
      value: ThreeVector2
    }
    time: {
      value: number
    }
  }
  dispose?: () => void
}

interface ThreeMesh {
  geometry: ThreeGeometry
}

interface ThreeVector2 {
  set: (x: number, y: number) => void
}

interface ThreeVector3 {}

declare global {
  interface Window {
    THREE?: ThreeModule
  }
}

const THREE_SCRIPT_SRC = 'https://unpkg.com/three@0.128.0/build/three.min.js'
const externalScriptCache = new Map<string, Promise<void>>()

const loadExternalScript = (src: string) => {
  if (typeof document === 'undefined') {
    return Promise.resolve()
  }

  const cached = externalScriptCache.get(src)
  if (cached) {
    return cached
  }

  const existing = document.querySelector<HTMLScriptElement>(`script[data-login-bg-script="${src}"]`)
  if (existing?.dataset.loaded === 'true') {
    return Promise.resolve()
  }

  const promise = new Promise<void>((resolve, reject) => {
    const script = existing ?? document.createElement('script')

    const handleLoad = () => {
      script.dataset.loaded = 'true'
      resolve()
    }

    const handleError = () => {
      reject(new Error(`Unable to load script: ${src}`))
    }

    script.src = src
    script.async = true
    script.dataset.loginBgScript = src
    script.addEventListener('load', handleLoad, { once: true })
    script.addEventListener('error', handleError, { once: true })

    if (!existing) {
      document.head.appendChild(script)
    }
  })

  externalScriptCache.set(src, promise)
  return promise
}

class WaterRippleBackground {
  private readonly mountElement: HTMLElement
  private readonly settings = {
    damping: 0.98,
    tension: 0.02,
    resolution: 512,
    rippleStrength: 1.0,
    mouseIntensity: 0.3,
    clickIntensity: 2.0,
    rippleRadius: 20,
    autoDrops: true,
    autoDropInterval: 3200,
    autoDropIntensity: 0.92,
  }
  private readonly gradientColors = {
    colorA1: [0.8, 0.3, 0.1],
    colorA2: [0.5, 0.1, 0.3],
    colorB1: [0.9, 0.6, 0.3],
    colorB2: [0.6, 0.2, 0.5],
  }
  private readonly lastMousePosition = {
    x: 0,
    y: 0,
  }

  private renderer: ThreeRenderer | null = null
  private scene: ThreeScene | null = null
  private camera: ThreeCamera | null = null
  private clock: ThreeClock | null = null
  private backgroundMesh: ThreeMesh | null = null
  private backgroundMaterial: ThreeShaderMaterial | null = null
  private waterTexture: ThreeDataTexture | null = null
  private waterBuffers: {
    current: Float32Array
    previous: Float32Array
  } | null = null
  private three: ThreeModule | null = null
  private mouseThrottleTime = 0
  private animationFrameId = 0
  private autoDropsInterval: number | null = null

  constructor(mountElement: HTMLElement) {
    this.mountElement = mountElement
  }

  public async init() {
    await loadExternalScript(THREE_SCRIPT_SRC)

    if (!window.THREE) {
      throw new Error('THREE failed to load')
    }

    this.three = window.THREE
    const THREE = this.three

    this.renderer = new THREE.WebGLRenderer({
      antialias: true,
      powerPreference: 'high-performance',
    })
    this.renderer.setSize(window.innerWidth, window.innerHeight)
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    this.mountElement.appendChild(this.renderer.domElement)

    this.scene = new THREE.Scene()
    this.camera = new THREE.OrthographicCamera(
      -window.innerWidth / 2,
      window.innerWidth / 2,
      window.innerHeight / 2,
      -window.innerHeight / 2,
      0.1,
      1000
    )
    this.camera.position.z = 10
    this.clock = new THREE.Clock()

    this.initWaterRipple()
    this.createBackground()
    this.bindEvents()
    this.setupAutoDrops()
    this.addRipple(window.innerWidth / 2, window.innerHeight / 2, 1.2)
    this.tick()
  }

  public destroy() {
    window.removeEventListener('mousemove', this.handleMouseMove)
    window.removeEventListener('click', this.handleClick)
    window.removeEventListener('resize', this.handleResize)

    if (this.autoDropsInterval !== null) {
      window.clearInterval(this.autoDropsInterval)
      this.autoDropsInterval = null
    }

    window.cancelAnimationFrame(this.animationFrameId)
    this.backgroundMesh?.geometry.dispose()
    this.backgroundMaterial?.dispose?.()
    this.waterTexture?.dispose?.()
    this.renderer?.dispose()
    this.mountElement.innerHTML = ''
  }

  private initWaterRipple() {
    if (!this.three) {
      return
    }

    const resolution = this.settings.resolution
    this.waterBuffers = {
      current: new Float32Array(resolution * resolution),
      previous: new Float32Array(resolution * resolution),
    }

    const textureFormat = this.three.RedFormat ?? this.three.LuminanceFormat
    if (textureFormat === undefined) {
      throw new Error('No compatible single-channel texture format available')
    }

    this.waterTexture = new this.three.DataTexture(
      this.waterBuffers.current,
      resolution,
      resolution,
      textureFormat,
      this.three.FloatType
    )
    this.waterTexture.minFilter = this.three.LinearFilter
    this.waterTexture.magFilter = this.three.LinearFilter
    this.waterTexture.needsUpdate = true
  }

  private createBackground() {
    if (!this.three || !this.scene || !this.waterTexture) {
      return
    }

    const backgroundShader = {
      uniforms: {
        waterTexture: { value: this.waterTexture },
        rippleStrength: { value: this.settings.rippleStrength },
        resolution: {
          value: new this.three.Vector2(window.innerWidth, window.innerHeight),
        },
        time: { value: 0 },
        colorA1: {
          value: new this.three.Vector3(
            this.gradientColors.colorA1[0],
            this.gradientColors.colorA1[1],
            this.gradientColors.colorA1[2]
          ),
        },
        colorA2: {
          value: new this.three.Vector3(
            this.gradientColors.colorA2[0],
            this.gradientColors.colorA2[1],
            this.gradientColors.colorA2[2]
          ),
        },
        colorB1: {
          value: new this.three.Vector3(
            this.gradientColors.colorB1[0],
            this.gradientColors.colorB1[1],
            this.gradientColors.colorB1[2]
          ),
        },
        colorB2: {
          value: new this.three.Vector3(
            this.gradientColors.colorB2[0],
            this.gradientColors.colorB2[1],
            this.gradientColors.colorB2[2]
          ),
        },
      },
      vertexShader: `
        varying vec2 vUv;
        void main() {
          vUv = uv;
          gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
        }
      `,
      fragmentShader: `
        uniform sampler2D waterTexture;
        uniform float rippleStrength;
        uniform vec2 resolution;
        uniform float time;
        uniform vec3 colorA1;
        uniform vec3 colorA2;
        uniform vec3 colorB1;
        uniform vec3 colorB2;
        varying vec2 vUv;

        float S(float a, float b, float t) {
          return smoothstep(a, b, t);
        }

        mat2 Rot(float a) {
          float s = sin(a);
          float c = cos(a);
          return mat2(c, -s, s, c);
        }

        float noise(vec2 p) {
          vec2 ip = floor(p);
          vec2 fp = fract(p);
          float a = fract(sin(dot(ip, vec2(12.9898, 78.233))) * 43758.5453);
          float b = fract(sin(dot(ip + vec2(1.0, 0.0), vec2(12.9898, 78.233))) * 43758.5453);
          float c = fract(sin(dot(ip + vec2(0.0, 1.0), vec2(12.9898, 78.233))) * 43758.5453);
          float d = fract(sin(dot(ip + vec2(1.0, 1.0), vec2(12.9898, 78.233))) * 43758.5453);
          fp = fp * fp * (3.0 - 2.0 * fp);
          return mix(mix(a, b, fp.x), mix(c, d, fp.x), fp.y);
        }

        void main() {
          float step = 1.0 / resolution.x;
          vec2 distortion = vec2(
            texture2D(waterTexture, vec2(vUv.x + step, vUv.y)).r - texture2D(waterTexture, vec2(vUv.x - step, vUv.y)).r,
            texture2D(waterTexture, vec2(vUv.x, vUv.y + step)).r - texture2D(waterTexture, vec2(vUv.x, vUv.y - step)).r
          ) * rippleStrength * 5.0;

          vec2 tuv = vUv + distortion;
          tuv -= 0.5;

          float ratio = resolution.x / resolution.y;
          tuv.y *= 1.0 / ratio;

          vec3 layer1 = mix(colorA1, colorA2, S(-0.3, 0.2, (tuv * Rot(radians(-5.0))).x));
          vec3 layer2 = mix(colorB1, colorB2, S(-0.3, 0.2, (tuv * Rot(radians(-5.0))).x));
          vec3 finalComp = mix(layer1, layer2, S(0.5, -0.3, tuv.y));

          float noiseValue = noise(tuv * 20.0 + time * 0.1) * 0.03;
          finalComp += vec3(noiseValue);

          float vignette = 1.0 - smoothstep(0.5, 1.5, length(tuv * 1.5));
          finalComp *= mix(0.95, 1.0, vignette);

          gl_FragColor = vec4(finalComp, 1.0);
        }
      `,
    }

    const geometry = new this.three.PlaneGeometry(window.innerWidth, window.innerHeight)
    this.backgroundMaterial = new this.three.ShaderMaterial({
      uniforms: backgroundShader.uniforms,
      vertexShader: backgroundShader.vertexShader,
      fragmentShader: backgroundShader.fragmentShader,
    })
    this.backgroundMesh = new this.three.Mesh(geometry, this.backgroundMaterial)
    this.scene.add(this.backgroundMesh)
  }

  private updateWaterSimulation() {
    if (!this.waterBuffers || !this.waterTexture) {
      return
    }

    const { current, previous } = this.waterBuffers
    const { damping, tension, resolution } = this.settings
    const safeTension = Math.min(tension, 0.05)

    for (let i = 1; i < resolution - 1; i += 1) {
      for (let j = 1; j < resolution - 1; j += 1) {
        const index = i * resolution + j
        const top = previous[index - resolution]
        const bottom = previous[index + resolution]
        const left = previous[index - 1]
        const right = previous[index + 1]

        current[index] = (top + bottom + left + right) / 2 - current[index]
        current[index] = current[index] * damping + previous[index] * (1 - damping)
        current[index] += (0 - previous[index]) * safeTension
        current[index] = Math.max(-1.0, Math.min(1.0, current[index]))
      }
    }

    this.waterBuffers = {
      current: previous,
      previous: current,
    }
    this.waterTexture.image.data = this.waterBuffers.current
    this.waterTexture.needsUpdate = true
  }

  private addRipple(x: number, y: number, strength = 1.0) {
    if (!this.waterBuffers) {
      return
    }

    const { resolution, rippleRadius } = this.settings
    const normalizedX = x / window.innerWidth
    const normalizedY = 1.0 - y / window.innerHeight
    const texX = Math.floor(normalizedX * resolution)
    const texY = Math.floor(normalizedY * resolution)
    const radiusSquared = rippleRadius * rippleRadius

    for (let i = -rippleRadius; i <= rippleRadius; i += 1) {
      for (let j = -rippleRadius; j <= rippleRadius; j += 1) {
        const distanceSquared = i * i + j * j
        if (distanceSquared > radiusSquared) {
          continue
        }

        const posX = texX + i
        const posY = texY + j
        if (posX < 0 || posX >= resolution || posY < 0 || posY >= resolution) {
          continue
        }

        const index = posY * resolution + posX
        const distance = Math.sqrt(distanceSquared)
        const rippleValue = Math.cos(((distance / rippleRadius) * Math.PI) / 2) * strength
        this.waterBuffers.previous[index] += rippleValue
      }
    }
  }

  private readonly handleMouseMove = (event: MouseEvent) => {
    const now = performance.now()
    if (now - this.mouseThrottleTime < 16) {
      return
    }

    this.mouseThrottleTime = now
    const x = event.clientX
    const y = event.clientY
    const dx = x - this.lastMousePosition.x
    const dy = y - this.lastMousePosition.y
    const distSquared = dx * dx + dy * dy

    if (distSquared > 5) {
      this.addRipple(x, y, this.settings.mouseIntensity)
      this.lastMousePosition.x = x
      this.lastMousePosition.y = y
    }
  }

  private readonly handleClick = (event: MouseEvent) => {
    this.addRipple(event.clientX, event.clientY, this.settings.clickIntensity)
  }

  private readonly handleResize = () => {
    if (!this.renderer || !this.camera || !this.backgroundMaterial || !this.backgroundMesh || !this.three) {
      return
    }

    const width = window.innerWidth
    const height = window.innerHeight

    this.camera.left = -width / 2
    this.camera.right = width / 2
    this.camera.top = height / 2
    this.camera.bottom = -height / 2
    this.camera.updateProjectionMatrix()
    this.renderer.setSize(width, height)
    this.backgroundMaterial.uniforms.resolution.value.set(width, height)

    this.backgroundMesh.geometry.dispose()
    this.backgroundMesh.geometry = new this.three.PlaneGeometry(width, height)
  }

  private bindEvents() {
    window.addEventListener('mousemove', this.handleMouseMove, { passive: true })
    window.addEventListener('click', this.handleClick, { passive: true })
    window.addEventListener('resize', this.handleResize, { passive: true })
  }

  private setupAutoDrops() {
    if (!this.settings.autoDrops) {
      return
    }

    this.autoDropsInterval = window.setInterval(() => {
      const x = Math.random() * window.innerWidth
      const y = Math.random() * window.innerHeight
      this.addRipple(x, y, this.settings.autoDropIntensity)
    }, this.settings.autoDropInterval)
  }

  private tick() {
    if (!this.renderer || !this.scene || !this.camera || !this.backgroundMaterial || !this.clock) {
      return
    }

    this.updateWaterSimulation()
    this.backgroundMaterial.uniforms.rippleStrength.value = this.settings.rippleStrength
    this.backgroundMaterial.uniforms.time.value += this.clock.getDelta()
    this.renderer.render(this.scene, this.camera)
    this.animationFrameId = window.requestAnimationFrame(() => this.tick())
  }
}

const sceneMountRef = ref<HTMLElement | null>(null)
const sceneInstanceRef = ref<WaterRippleBackground | null>(null)

onMounted(async () => {
  if (!sceneMountRef.value) {
    return
  }

  try {
    const scene = new WaterRippleBackground(sceneMountRef.value)
    sceneInstanceRef.value = scene
    await scene.init()
  } catch (error) {
    console.error('Failed to initialize login water background.', error)
  }
})

onBeforeUnmount(() => {
  sceneInstanceRef.value?.destroy()
  sceneInstanceRef.value = null
})
</script>

<style scoped>
.login-water-background,
.login-water-canvas {
  position: absolute;
  inset: 0;
}

.login-water-background {
  overflow: hidden;
  background:
    radial-gradient(circle at 16% 18%, rgba(255, 180, 80, 0.18), transparent 22%),
    radial-gradient(circle at 74% 22%, rgba(255, 107, 53, 0.18), transparent 24%),
    #050505;
}

.login-water-canvas :deep(canvas) {
  display: block;
  width: 100%;
  height: 100%;
}
</style>
