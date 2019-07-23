package com.varutra.webscarab.plugin.fragments;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.nodes.RemarkNode;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.ScriptTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import com.varutra.webscarab.model.ConversationID;
import com.varutra.webscarab.model.HttpUrl;
import com.varutra.webscarab.model.Request;
import com.varutra.webscarab.model.Response;
import com.varutra.webscarab.model.StoreException;
import com.varutra.webscarab.parser.Parser;
import com.varutra.webscarab.plugin.Framework;
import com.varutra.webscarab.plugin.Hook;
import com.varutra.webscarab.plugin.Plugin;

public class Fragments implements Plugin {
    
    private Logger _logger = Logger.getLogger(getClass().getName());
    
    private FragmentsModel _model = null;
    Pattern[] jsDomXssPatterns = {
            Pattern.compile("[\\S&&[^=]]+\\s*=\\s*window\\.(?:top\\.)?location"),
            Pattern
                    .compile("[\\S&&[^=]]+\\s*=\\s*document\\.(?:URL|URLUnencoded|location)"), 
            
            Pattern.compile("\\+\\s*window\\.(?:top\\.)?location"),
            Pattern.compile("\\+\\s*document\\.(?:URL|URLUnencoded|location)"),
    };
    
    Pattern[] jsDomXssFalsePositivesPattern = {
            Pattern.compile(".+[!=]+=.*(?:document|window)"),
            Pattern.compile("escape\\((?:document.|window.).+\\)"),
    };
    
    public Fragments(Framework framework) {
        _model = new FragmentsModel(framework.getModel());
    }
    
    public FragmentsModel getModel() {
        return _model;
    }
    
    public void setSession(String type, Object store, String session) throws StoreException {
        if (type.equals("FileSystem") && (store instanceof File)) {
            _model.setStore(new FileSystemStore((File) store, session));
        } else {
            throw new StoreException("Store type '" + type + "' is not supported in " + getClass().getName());
        }
    }
    public String getPluginName() {
        return "Fragments";
    }
    public void run() {
        _model.setRunning(true);
    }
    public boolean stop() {
        _model.setRunning(false);
        return ! _model.isRunning();
    }
    
    public void analyse(ConversationID id, Request request, Response response, String origin) {
        HttpUrl url = request.getURL();
        Object parsed = Parser.parse(url, response);
        if (parsed != null && parsed instanceof NodeList) {
            NodeList nodes = (NodeList) parsed;
            try {
                NodeList comments = nodes.searchFor(RemarkNode.class);
                NodeList scripts = nodes.searchFor(ScriptTag.class);
                NodeList forms = nodes.searchFor(FormTag.class);
                NodeList inputs = nodes.searchFor(InputTag.class);
            
                for (NodeIterator ni = comments.elements(); ni.hasMoreNodes(); ) {
                    String fragment = ni.nextNode().toHtml();
                    _model.addFragment(url, id, FragmentsModel.KEY_COMMENTS, fragment);
                }
                for (NodeIterator ni = scripts.elements(); ni.hasMoreNodes(); ) {
                    String fragment = ni.nextNode().toHtml();
                    _model.addFragment(url, id, FragmentsModel.KEY_SCRIPTS, fragment);
                }
                for (NodeIterator ni = forms.elements(); ni.hasMoreNodes(); ) {
                    FormTag form = (FormTag) ni.nextNode();
                    String fragment = "action:"+form.getAttribute("action")+" method:"+form.getAttribute("method");
                    _model.addFragment(url, id, FragmentsModel.KEY_FORMS,fragment );
                }
                for (NodeIterator ni = inputs.elements(); ni.hasMoreNodes(); ) {
                    InputTag tag = (InputTag) ni.nextNode();
                    String type = tag.getAttribute("type");
                    if( "hidden".equals(type))
                    {
                        String fragment = tag.toHtml();
                        _model.addFragment(url, id, FragmentsModel.KEY_HIDDENFIELD, fragment);
                    }
                    if("file".equals(type))
                    {
                        String fragment = tag.toHtml();
                        _model.addFragment(url, id, FragmentsModel.KEY_FILEUPLOAD, fragment);
                    }
                }
            } catch (ParserException pe) {
                _logger.warning("Looking for fragments, got '" + pe + "'");
            }
        }
        try {
            String content = new String(response.getContent(),"UTF-8");
            for (int i = 0; i < jsDomXssPatterns.length; i++) {
                Matcher m = jsDomXssPatterns[i].matcher(content);
                while(m.find())
                {
                    String fragment = m.group();
                    boolean falsePositive = false;
                    for (int j = 0; j < jsDomXssFalsePositivesPattern.length; j++) {
                        Matcher fp = jsDomXssFalsePositivesPattern[j]
                                .matcher(fragment);
                        if (fp.find()) {
                            falsePositive = true;
                            _logger
                                    .info("Ignoring XSS-DOM fragment '"
                                            + fragment
                                            + "' - false positive according to pattern :"
                                            + jsDomXssFalsePositivesPattern[j]
                                                    .pattern());
                            break;
                        }
                    }
                    if (!falsePositive)
                    {
                        _model.addFragment(url, id, FragmentsModel.KEY_DOMXSS,
                                fragment);
                    }
                    
                }
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        
    }
    public void flush() throws StoreException {
        _model.flush();
    }
    
    public boolean isBusy() {
        return _model.isBusy();
    }
    
    public String getStatus() {
        return _model.getStatus();
    }
    
    public boolean isModified() {
        return _model.isModified();
    }
    
    public boolean isRunning() {
        return _model.isRunning();
    }
    
    public Object getScriptableObject() {
        return null;
    }
    
    public Hook[] getScriptingHooks() {
        return new Hook[0];
    }
    
}

