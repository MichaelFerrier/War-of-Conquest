using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
#if UNITY_IOS || UNITY_ANDROID
using EasyMobile;
#endif

public class SharePanel : MonoBehaviour
{
    public static SharePanel instance;
    public GameObject progressIndicator;
    public Image progressIndicatorImage, giphyImage;
    public Image shareImage;
    public LayoutElement clipPlayerUILayout, shareImageLayout;
    public Texture2D texture = null;

    const float MAX_WIDTH = 350;
    const float MAX_HEIGHT = 350;

  public SharePanel()
  {
    instance = this;
  }

  public void OnEnable()
  {
    // Start with progress indicator turned off.
    progressIndicator.SetActive(false);
  }

#if UNITY_IOS || UNITY_ANDROID
  public AnimatedClip gifClip = null;
  public ClipPlayerUI clipPlayerUI;

    public void OnDisable()
    {
        texture = null;

        if (gifClip != null)
        {
            gifClip.Dispose();
            gifClip = null;
        }
    }

    public void InitForTexture2D(Texture2D _texture)
    {
        // Show the appropriate display object
        shareImage.gameObject.SetActive(true);
        clipPlayerUI.gameObject.SetActive(false);
        giphyImage.gameObject.SetActive(false);

        // Display the given texture
        texture = _texture;
        float scale = Mathf.Min(MAX_WIDTH / _texture.width, MAX_HEIGHT / _texture.height);
        shareImage.sprite = Sprite.Create(_texture, new Rect(0f, 0f, _texture.width, _texture.height), new Vector2(0f,0f));
        shareImageLayout.preferredWidth = scale * _texture.width;
        shareImageLayout.preferredHeight = scale * _texture.height;
    }

    public void InitForGifClip(AnimatedClip _gifClip)
    {
        gifClip = _gifClip;

        // Show the appropriate display object
        shareImage.gameObject.SetActive(false);
        clipPlayerUI.gameObject.SetActive(true);
        giphyImage.gameObject.SetActive(true);

        // Set dimensions of clipPlayerUI
        clipPlayerUILayout.preferredHeight = gifClip.Height  * clipPlayerUILayout.preferredWidth / gifClip.Width;

        StartCoroutine(InitForGifClip_Coroutine());
    }

    public IEnumerator InitForGifClip_Coroutine()
    {
        // Wait until next frame, when the clipPlayerUI is active, before playing the clip.
        yield return null;

        // Display the given clip
        Gif.PlayClip(clipPlayerUI, gifClip);
    }

    public void OnClick_Okay()
    {
        if (texture != null)
        {
            // Use the device's native share
            Sharing.ShareTexture2D(texture, "screenshot", "(War of Conquest patron code: " + GameData.instance.patronCode + ")");

            // Close the share panel
            GameGUI.instance.CloseAllPanels();
        }
        else if (gifClip != null)
        {
            // Parameter setup
            string filename = "capture.gif";
            int loop = 0; // -1: no loop, 0: loop indefinitely, >0: loop a set number of times
            int quality = 80; // 80 is a good value in terms of time-quality balance
            System.Threading.ThreadPriority tPriority = System.Threading.ThreadPriority.AboveNormal; // exporting thread priority

            // Begin export of gif
            Gif.ExportGif(gifClip, filename, loop, quality, tPriority, OnGifExportProgress, OnGifExportCompleted);

            // Show progress indicator
            progressIndicator.SetActive(true);
            progressIndicatorImage.fillAmount = 0;
        }
    }

    // This callback is called repeatedly during the GIF exporting process.
    // It receives a reference to original clip and a progress value ranging from 0 to 1.
    void OnGifExportProgress(AnimatedClip clip, float progress)
    {
        // Update progress indicator
        progressIndicatorImage.fillAmount = progress * 0.5f;
    }

    // This callback is called once the GIF exporting has completed.
    // It receives a reference to the original clip and the filepath of the generated image.
    void OnGifExportCompleted(AnimatedClip clip, string path)
    {
        // The GIF image has been created, now we'll upload it to Giphy
        // First prepare the upload content
        var content = new GiphyUploadParams();
        content.localImagePath = path; // the file path of the generated GIF image
        content.tags = GameData.instance.username + ", " + GameData.instance.nationName + ", War of Conquest, IronZog"; // optional image tags, comma-delimited
        content.sourcePostUrl = ""; // optional image source, e.g. your website
        content.isHidden = false; // optional hidden flag, set to true to mark the image as private

        // Upload the image to Giphy using the public beta key
        Giphy.Upload(content, OnGiphyUploadProgress, OnGiphyUploadCompleted, OnGiphyUploadFailed);
    }

    // This callback is called repeatedly during the uploading process.
    // It receives a progress value ranging from 0 to 1.
    void OnGiphyUploadProgress(float progress)
    {
        // Update progress indicator
        progressIndicatorImage.fillAmount = 0.5f + (progress * 0.5f);
    }

    // This callback is called once the uploading has completed.
    // It receives the URL of the uploaded image.
    void OnGiphyUploadCompleted(string url)
    {
        // Open native share interface to share URL of uploaded gif file
        Sharing.ShareURL(url);

        // Close the share panel
        GameGUI.instance.CloseAllPanels();
    }

    // This callback is called if the upload has failed.
    // It receives the error message.
    void OnGiphyUploadFailed(string error)
    {
        // Display error message
        GameGUI.instance.DisplayMessage("Failed to upload Gif: " + error);
        Debug.Log("Uploading to Giphy has failed with error: " + error);

        // Close the share panel
        GameGUI.instance.CloseAllPanels();
    }
#endif
}
