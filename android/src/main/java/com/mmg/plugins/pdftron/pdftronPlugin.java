package com.mmg.plugins.pdftron;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.getcapacitor.JSObject;
import com.getcapacitor.Logger;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.config.ToolManagerBuilder;
import com.pdftron.pdf.config.ViewerConfig;
import com.pdftron.pdf.controls.PdfViewCtrlTabFragment;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

@CapacitorPlugin(name = "PDFTron")
public class PDFTronPlugin extends Plugin {

    private static final String TAG = "PDFTron";

    // options
    public static final String Key_initialDoc = "initialDoc";
    public static final String Key_password = "password";
    public static final String Key_boundingRect = "boundingRect";
    public static final String Key_disabledElements = "disabledElements";
    public static final String Key_showNavIcon = "showTopLeftButton";
    public static final String Key_navIconTitle = "topLeftButtonTitle";

    // methods
    public static final String Key_showDocumentViewer = "showDocumentViewer";
    public static final String Key_disableElements = "disableElements";
    public static final String Key_enableTools = "enableTools";
    public static final String Key_disableTools = "disableTools";
    public static final String Key_setToolMode = "setToolMode";
    public static final String Key_setPagePresentationMode = "setPagePresentationMode";
    public static final String Key_loadDocument = "loadDocument";
    public static final String Key_save = "save";
    public static final String Key_NativeViewer = "NativeViewer";

    // nav
    public static final String Key_close = "close";
    public static final String Key_menu = "menu";
    public static final String Key_back = "back";

    private DocumentView mDocumentView;
    private ViewerConfig.Builder mBuilder;
    private ToolManagerBuilder mToolManagerBuilder;

    // 对外方法-基本方法（示例）
    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        Logger.info("Echo", value);
        ret.put("value", value);
        call.resolve(ret);
    }

    // 对外方法-初始化
    @PluginMethod
    public void initialize(PluginCall call) {
        mBuilder = new ViewerConfig.Builder()
                .useSupportActionBar(false)
                .fullscreenModeEnabled(false)
                .multiTabEnabled(false)
                .saveCopyExportPath(getContext().getCacheDir().getAbsolutePath())
                .openUrlCachePath(getContext().getCacheDir().getAbsolutePath());
        mToolManagerBuilder = ToolManagerBuilder.from();

        // 获取配置
        String options = call.getString("settings");
        // 获取加载组件
        String viewerElement = call.getString("viewerElement");
        // 创建Viewer
        addDocumentViewer(options, viewerElement, call);
    }

    // 添加文档视图
    private void addDocumentViewer(String options, String viewerElement, PluginCall call) {
        try {
            final JSONObject jsonObject = new JSONObject(options);
            getActivity().runOnUiThread(() -> createDocumentViewerImpl(jsonObject, viewerElement, call));
        } catch (Exception e) {
            e.printStackTrace();
            call.reject(e.getMessage());
        }
    }

    // 创建文档视图实例
    private void createDocumentViewerImpl(@NonNull JSONObject options, @Nullable String viewerElement, PluginCall call) {
        try {
            Activity currentActivity = getActivity();
            if (currentActivity instanceof AppCompatActivity) {
                AppCompatActivity fragmentActivity = (AppCompatActivity) getActivity();

                mDocumentView = new DocumentView(getContext());
                mDocumentView.setSupportFragmentManager(fragmentActivity.getSupportFragmentManager());
                mDocumentView.setPlugin(this);

                // 配置-初始文档
                if (options.has(Key_initialDoc)) {
                    String initialDoc = options.getString(Key_initialDoc);
                    mDocumentView.setDocumentUri(Uri.parse(initialDoc));
                }

                if (options.has(Key_password)) {
                    String password = options.getString(Key_password);
                    mDocumentView.setPassword(password);
                }

                // 配置-渲染位置
                if (options.has(Key_boundingRect)) {
                    String rect = options.getString(Key_boundingRect);
                    Log.d("capacitor", "boundingRect: " + rect);
                    JSONObject rectObject = new JSONObject(rect);
                    int left = (int) Float.parseFloat(rectObject.getString("left"));
                    int top = (int) Float.parseFloat(rectObject.getString("top"));
                    int width = (int) Float.parseFloat(rectObject.getString("width"));
                    int height = (int) Float.parseFloat(rectObject.getString("height"));
                    mDocumentView.setRect((int) Utils.convDp2Pix(getContext(), left),
                            (int) Utils.convDp2Pix(getContext(), top),
                            (int) Utils.convDp2Pix(getContext(), width),
                            (int) Utils.convDp2Pix(getContext(), height));
                }

                // 配置-禁用组件
                if (options.has(Key_disabledElements)) {
                    disableElements(options.getJSONArray(Key_disabledElements));
                }

                // 配置-左上角按钮
                String navIcon = "ic_menu_white_24dp";
                if (options.has(Key_navIconTitle)) {
                    String title = options.getString(Key_navIconTitle);
                    if (Key_menu.equalsIgnoreCase(title)) {
                        navIcon = "ic_menu_white_24dp";
                    } else if (Key_back.equalsIgnoreCase(title)) {
                        navIcon = "ic_arrow_back_white_24dp";
                    } else if (Key_close.equalsIgnoreCase(title)) {
                        navIcon = "ic_close_white_24dp";
                    }
                }
                mDocumentView.setNavIconResName(navIcon);
                boolean showNav = true;
                // 是否展示左上角按钮
                if (options.has(Key_showNavIcon)) {
                    showNav = options.getBoolean(Key_showNavIcon);
                }
                mDocumentView.setShowNavIcon(showNav);

                if (!Utils.isNullOrEmpty(viewerElement)) {
                    attachDocumentViewerImpl();
                }
                call.resolve();
            } else {
                call.reject("Current activity is not instanceof FragmentActivity");
            }
        } catch (Exception e) {
            e.printStackTrace();
            call.reject(e.getMessage());
        }
    }

    // 隐藏组件
    private void disableElements(JSONArray args) throws JSONException {
        for (int i = 0; i < args.length(); i++) {
            String item = args.getString(i);
            if ("toolsButton".equals(item)) {
                mBuilder = mBuilder.showAnnotationToolbarOption(false);
            } else if ("searchButton".equals(item)) {
                mBuilder = mBuilder.showSearchView(false);
            } else if ("shareButton".equals(item)) {
                mBuilder = mBuilder.showShareOption(false);
            } else if ("viewControlsButton".equals(item)) {
                mBuilder = mBuilder.showDocumentSettingsOption(false);
            } else if ("thumbnailsButton".equals(item)) {
                mBuilder = mBuilder.showThumbnailView(false);
            } else if ("listsButton".equals(item)) {
                mBuilder = mBuilder
                        .showAnnotationsList(false)
                        .showOutlineList(false)
                        .showUserBookmarksList(false);
            } else if ("thumbnailSlider".equals(item)) {
                mBuilder = mBuilder.showBottomNavBar(false);
            } else if ("editPagesButton".equals(item)) {
                mBuilder = mBuilder.showEditPagesOption(false);
            } else if ("printButton".equals(item)) {
                mBuilder = mBuilder.showPrintOption(false);
            } else if ("closeButton".equals(item)) {
                mBuilder = mBuilder.showCloseTabOption(false);
            } else if ("saveCopyButton".equals(item)) {
                mBuilder = mBuilder.showSaveCopyOption(false);
            } else if ("formToolsButton".equals(item)) {
                mBuilder = mBuilder.showFormToolbarOption(false);
            } else if ("moreItemsButton".equals(item)) {
                mBuilder = mBuilder
                        .showEditPagesOption(false)
                        .showPrintOption(false)
                        .showCloseTabOption(false)
                        .showSaveCopyOption(false)
                        .showFormToolbarOption(false);
            }
        }
        disableTools(args);
    }

    // 隐藏工具栏
    private void disableTools(JSONArray args) throws JSONException {
        ArrayList<ToolManager.ToolMode> modesArr = new ArrayList<>();
        for (int i = 0; i < args.length(); i++) {
            String item = args.getString(i);
            ToolManager.ToolMode mode = convStringToToolMode(item);
            if (mode != null) {
                modesArr.add(mode);
            }
        }
        ToolManager.ToolMode[] modes = modesArr.toArray(new ToolManager.ToolMode[modesArr.size()]);
        if (mDocumentView.mPdfViewCtrlTabHostFragment != null && mDocumentView.mPdfViewCtrlTabHostFragment.getCurrentPdfViewCtrlFragment() != null) {
            ToolManager toolManager = mDocumentView.mPdfViewCtrlTabHostFragment.getCurrentPdfViewCtrlFragment().getToolManager();
            if (toolManager != null) {
                toolManager.disableToolMode(modes);
            }
        } else {
            mToolManagerBuilder = mToolManagerBuilder.disableToolModes(modes);
        }
    }

    // 工具栏类型转换
    private ToolManager.ToolMode convStringToToolMode(String item) {
        ToolManager.ToolMode mode = null;
        if ("freeHandToolButton".equals(item) || "AnnotationCreateFreeHand".equals(item)) {
            mode = ToolManager.ToolMode.INK_CREATE;
        } else if ("highlightToolButton".equals(item) || "AnnotationCreateTextHighlight".equals(item)) {
            mode = ToolManager.ToolMode.TEXT_HIGHLIGHT;
        } else if ("underlineToolButton".equals(item) || "AnnotationCreateTextUnderline".equals(item)) {
            mode = ToolManager.ToolMode.TEXT_UNDERLINE;
        } else if ("squigglyToolButton".equals(item) || "AnnotationCreateTextSquiggly".equals(item)) {
            mode = ToolManager.ToolMode.TEXT_SQUIGGLY;
        } else if ("strikeoutToolButton".equals(item) || "AnnotationCreateTextStrikeout".equals(item)) {
            mode = ToolManager.ToolMode.TEXT_STRIKEOUT;
        } else if ("rectangleToolButton".equals(item) || "AnnotationCreateRectangle".equals(item)) {
            mode = ToolManager.ToolMode.RECT_CREATE;
        } else if ("ellipseToolButton".equals(item) || "AnnotationCreateEllipse".equals(item)) {
            mode = ToolManager.ToolMode.OVAL_CREATE;
        } else if ("lineToolButton".equals(item) || "AnnotationCreateLine".equals(item)) {
            mode = ToolManager.ToolMode.LINE_CREATE;
        } else if ("arrowToolButton".equals(item) || "AnnotationCreateArrow".equals(item)) {
            mode = ToolManager.ToolMode.ARROW_CREATE;
        } else if ("polylineToolButton".equals(item) || "AnnotationCreatePolyline".equals(item)) {
            mode = ToolManager.ToolMode.POLYLINE_CREATE;
        } else if ("polygonToolButton".equals(item) || "AnnotationCreatePolygon".equals(item)) {
            mode = ToolManager.ToolMode.POLYGON_CREATE;
        } else if ("cloudToolButton".equals(item) || "AnnotationCreatePolygonCloud".equals(item)) {
            mode = ToolManager.ToolMode.CLOUD_CREATE;
        } else if ("signatureToolButton".equals(item) || "AnnotationCreateSignature".equals(item)) {
            mode = ToolManager.ToolMode.SIGNATURE;
        } else if ("freeTextToolButton".equals(item) || "AnnotationCreateFreeText".equals(item)) {
            mode = ToolManager.ToolMode.TEXT_CREATE;
        } else if ("stickyToolButton".equals(item) || "AnnotationCreateSticky".equals(item)) {
            mode = ToolManager.ToolMode.TEXT_ANNOT_CREATE;
        } else if ("calloutToolButton".equals(item) || "AnnotationCreateCallout".equals(item)) {
            mode = ToolManager.ToolMode.CALLOUT_CREATE;
        } else if ("stampToolButton".equals(item) || "AnnotationCreateStamp".equals(item)) {
            mode = ToolManager.ToolMode.STAMPER;
        } else if ("AnnotationCreateDistanceMeasurement".equals(item)) {
            mode = ToolManager.ToolMode.RULER_CREATE;
        } else if ("AnnotationCreatePerimeterMeasurement".equals(item)) {
            mode = ToolManager.ToolMode.PERIMETER_MEASURE_CREATE;
        } else if ("AnnotationCreateAreaMeasurement".equals(item)) {
            mode = ToolManager.ToolMode.AREA_MEASURE_CREATE;
        } else if ("TextSelect".equals(item)) {
            mode = ToolManager.ToolMode.TEXT_SELECT;
        } else if ("AnnotationEdit".equals(item)) {
            mode = ToolManager.ToolMode.ANNOT_EDIT_RECT_GROUP;
        }
        return mode;
    }

    // 添加文档视图实例
    private void attachDocumentViewerImpl() throws PDFNetException {
        if (mDocumentView == null) {
            return;
        }
        mDocumentView.setVisibility(View.VISIBLE);
        if (mDocumentView.getParent() != null) {
            return;
        }
        mDocumentView.setViewerConfig(getViewerConfig());
        if (bridge.getWebView() instanceof WebView) {
            WebView wv = (WebView) bridge.getWebView();
            if (wv.getParent() != null && wv.getParent() instanceof ViewGroup) {
                ((ViewGroup) wv.getParent()).addView(mDocumentView);
            } else {
                wv.addView(mDocumentView);
            }
        } else {
            throw new PDFNetException("CapacitorWebView is not instanceof WebView", -1, "PDFTron.java", "attachDocumentViewerImpl", "Unable to add viewer.");
        }
    }

    // 获取ToolManager设置
    private ViewerConfig getViewerConfig() {
        return mBuilder
                .toolManagerBuilder(mToolManagerBuilder)
                .build();
    }

    // 隐藏视图
    public void hideView() {
        if (mDocumentView == null) {
            return;
        }
        mDocumentView.setVisibility(View.GONE);
    }

    // 接收监听信息
    public void fireJavascriptEvent(String action) {
        sendEventMessage(action);
    }

    // 发送监听消息给JS端
    private void sendEventMessage(String action) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("action", action);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create event message", e);
        }
    }

    // 对外方法-保存
    @PluginMethod
    public void saveDocument(PluginCall call) {
        if (mDocumentView != null && mDocumentView.mPdfViewCtrlTabHostFragment != null && mDocumentView.mPdfViewCtrlTabHostFragment.getCurrentPdfViewCtrlFragment() != null) {
            mDocumentView.mPdfViewCtrlTabHostFragment.getCurrentPdfViewCtrlFragment().save(false, true, true);

            JSObject ret = new JSObject();
            ret.put("filePath", mDocumentView.mPdfViewCtrlTabHostFragment.getCurrentPdfViewCtrlFragment().getFilePath());

            call.resolve(ret);
        } else {
            call.reject("Saving failed.");
        }
    }

    // 对外方法-关闭
}
