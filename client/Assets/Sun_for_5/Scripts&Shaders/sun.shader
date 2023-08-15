// Upgrade NOTE: replaced 'mul(UNITY_MATRIX_MVP,*)' with 'UnityObjectToClipPos(*)'

// Upgrade NOTE: replaced '_Object2World' with 'unity_ObjectToWorld'

Shader "Realistic sun/sun" {
    Properties {
        _Color ("Main Color", Color) = (1,1,1,0.5)
        _MainTex ("Texture", 2D) = "white" { }
        _Hue("Hue", Range(0.0,1.0)) = 0
        
        _Saturation("Saturation", Range(0.0,1.0)) = 0.5
        _Brightness("Brightness", Range(0.0,1.0)) = 0.0
        _Contour("Contour Brightness", Range(0.0,1.0)) = 0.5
        _Contrast("Contrast", Range(0.0,3.0)) = 0.5
    }
    SubShader {
 
        Pass {
           Tags{"Queue" ="Transparent" "RenderType"="Transparent"}
Lighting off
//Blend One One
//Blend OneMinusDstColor One
Blend SrcAlpha OneMinusSrcAlpha
        CGPROGRAM
        #pragma target 3.0
        #pragma vertex vert
        #pragma fragment frag

        #include "UnityCG.cginc"

        fixed4 _Color;
        sampler2D _MainTex;
        float _Contrast;
       float  _Brightness;
       float _Contour;
        float _Saturation;
 float _Hue;
  float Epsilon = 1e-10;

  
        struct v2f {
            float4 pos : SV_POSITION;
            float2 uv : TEXCOORD0;
            float3 normal : TEXCOORD1;
        };

        float4 _MainTex_ST;

        v2f vert (appdata_base v)
        {
            v2f o;
            o.normal = v.normal;
            o.pos = UnityObjectToClipPos (v.vertex);
            o.uv = TRANSFORM_TEX (v.texcoord, _MainTex);
            return o;
        }
  
  float3 HUEtoRGB(in float H)
  {
    float R = abs(H * 6 - 3) - 1;
    float G = 2 - abs(H * 6 - 2);
    float B = 2 - abs(H * 6 - 4);
    return saturate(float3(R,G,B));
  }

  float3 RGBtoHCV(in float3 RGB)
  {
    // Based on work by Sam Hocevar and Emil Persson
    float4 P = (RGB.g < RGB.b) ? float4(RGB.bg, -1.0, 2.0/3.0) : float4(RGB.gb, 0.0, -1.0/3.0);
    float4 Q = (RGB.r < P.x) ? float4(P.xyw, RGB.r) : float4(RGB.r, P.yzx);
    float C = Q.x - min(Q.w, Q.y);
    float H = abs((Q.w - Q.y) / (6 * C + Epsilon) + Q.z);
    return float3(H, C, Q.x);
  }
          
      float3 HSLtoRGB(in float3 HSL)
  {
    float3 RGB = HUEtoRGB(HSL.x);
    float C = (1 - abs(2 * HSL.z - 1)) * HSL.y;
    return (RGB - 0.5) * C + HSL.z;
  }

	float3 RGBtoHSL(in float3 RGB)
  {
    float3 HCV = RGBtoHCV(RGB);
    float L = HCV.z - HCV.y * 0.5;
    float S = HCV.y / (1 - abs(L * 2 - 1) + Epsilon);
    return float3(HCV.x, S, L);
  }

        fixed4 frag (v2f i) : SV_Target
        {
            fixed4 texcol = tex2D (_MainTex, i.uv);

          float glow =pow( 2.0-dot(-normalize((mul((float4x4)unity_ObjectToWorld, float4(0.0,0.0,0.0,1.0))-_WorldSpaceCameraPos)), normalize(mul((float3x3)unity_ObjectToWorld,i.normal))),(_Contour*4.0+2.0));
        
           float4 f_color=texcol * _Color*(_Brightness*5.0+1.0)*glow;
            f_color = f_color + 1.0-_Saturation;
   float3 hsl = RGBtoHSL(f_color.xyz);
             hsl.x= hsl.x+ _Hue;
         
            f_color.xyz = HSLtoRGB(float3(hsl.x,hsl.y,hsl.z));
            return clamp(f_color,1.0,f_color)/(1+_Contrast);;
        }
        ENDCG

        }
    }
}