using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
/*
public class FacebookUser : MonoBehaviour
{
    public enum ProcessingStates
    {
        Idle,
        LoggingIn,
        Posting
    }

    public static FacebookUser instance;
    public ProcessingStates state = ProcessingStates.Idle;

    public void Awake()
    {
        instance = this;

        SPFacebook.OnInitCompleteAction += OnInit;

        SPFacebook.OnAuthCompleteAction += OnAuth;
        SPFacebook.OnPostingCompleteAction += OnPost;

        Debug.Log("Facebook, initializing...");
        SPFacebook.Instance.Init();
    }

    // Use this for initialization
    void Start () {
    }

    // Update is called once per frame
    void Update () {
		
	}

    public void OnClick_FacebookPostScreenshot()
    {
        StartCoroutine(PostScreenshot());
    }

    private void OnAuth(FB_Result result)
    {
        if (SPFacebook.Instance.IsLoggedIn)
        {
            Debug.Log("Facebook, User logged into Facebook");
        }
        else
        {
            Debug.LogError("Facebook, User failed to log into Facebook. Error: " + result.Error);
        }
    }

    private void OnPost(FB_PostResult res)
    {
        state = ProcessingStates.Idle;

        if (res.IsSucceeded)
        {
            Debug.Log("Facebook, Posting complete");
            Debug.Log("Facebook, Post id: " + res.PostId);
        }
        else
        {
            Debug.LogError("Facebook, Oops, post failed, something was wrong");
            Debug.LogError("Facebook, Failed Post Error Data" + res.RawData);
        }
    }

    private void OnInit()
    {
        Debug.Log("Facebook, init complete");
        //if (SPFacebook.Instance.IsLoggedIn)
        //{
        //    //IsAuntificated = true;
        //}
        //else
        //{
        //    SA_StatusBar.text = "user Login -> false";
        //}
    }

    public IEnumerator PostScreenshot()
    {
        if (!SPFacebook.Instance.IsInited)
        {
            Debug.Log("Facebook, Something is wrong. The Facebook Plugin is not initialized!");
            yield break;
        }

        // check if teh user is logged, and go to login if not
        if (!SPFacebook.Instance.IsLoggedIn)
        {
            state = ProcessingStates.LoggingIn;
            SPFacebook.Instance.Login("publish_actions", "public_profile", "email");

            yield return new WaitUntil(UserIsDoneWithLogin);
        }

        // if the user is not logged in at this point, exit coroutine
        if (!SPFacebook.Instance.IsLoggedIn)
        {
            Debug.Log("Facebook, User failed to log in!");
            yield break;
        }

        state = ProcessingStates.Posting;;

        Debug.Log("In PostScreenshot");
        int width = Screen.width;
        int height = Screen.height;

        // Rect original_rect = MapView.instance.camera.rect;
        MapView.instance.camera.rect = new Rect(0, 0, 1, 1);

        // Create a RenderTexture and assign it to be the camera's render target.
        RenderTexture rt = new RenderTexture(width, height, 24, RenderTextureFormat.ARGB32);
        rt.antiAliasing = 8;
        rt.Create();
        MapView.instance.camera.targetTexture = rt;

        // Render to the RenderTexture, and wait unto the frame is complete.
        MapView.instance.camera.Render();
        yield return new WaitForEndOfFrame();

        // Make the RenderTexture active, so that ReadPixels will read from it rather than from the screen buffer.
        RenderTexture.active = rt;

        // Create a Texture2D and copy the image from the RenderTexture into it.
        Texture2D newTexture = new Texture2D(rt.width, rt.height);
        newTexture.ReadPixels(new Rect(0, 0, rt.width, rt.height), 0, 0);

        Debug.Log("before posting to Facebook");
        SPFacebook.Instance.PostImage("", newTexture);
    }

    private bool UserIsDoneWithLogin()
    {
        // allow coroutine to continue when the user is done logging in or quits the login process
        return state != ProcessingStates.LoggingIn;
    }

}
*/