package io.datonis.aliothmi.adapter;

import org.opcfoundation.ua.application.Application;
import org.opcfoundation.ua.application.Client;
import org.opcfoundation.ua.application.SessionChannel;
import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.builtintypes.Variant;
import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.Attributes;
import org.opcfoundation.ua.core.BrowseDescription;
import org.opcfoundation.ua.core.BrowseDirection;
import org.opcfoundation.ua.core.BrowseResponse;
import org.opcfoundation.ua.core.BrowseResult;
import org.opcfoundation.ua.core.BrowseResultMask;
import org.opcfoundation.ua.core.EndpointDescription;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.MessageSecurityMode;
import org.opcfoundation.ua.core.NodeClass;
import org.opcfoundation.ua.core.ReadResponse;
import org.opcfoundation.ua.core.ReadValueId;
import org.opcfoundation.ua.core.ReferenceDescription;
import org.opcfoundation.ua.core.TimestampsToReturn;
import org.opcfoundation.ua.core.WriteResponse;
import org.opcfoundation.ua.core.WriteValue;
import org.opcfoundation.ua.transport.security.Cert;
import org.opcfoundation.ua.transport.security.KeyPair;
import org.opcfoundation.ua.transport.security.PrivKey;
import org.opcfoundation.ua.utils.CertificateUtils;
import org.opcfoundation.ua.utils.EndpointUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.security.cert.CertificateException;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by mayank on 29/4/17.
 */

public class OPCUAAdapter extends BaseAdapter {

    private static Logger logger = LoggerFactory.getLogger(OPCUAAdapter.class);
    private static final String PRIVKEY_PASSWORD = "Opc.Ua";
    private SessionChannel session = null;
    protected volatile boolean connected = false;
    private String url = null;
    private boolean secure = true;
    private boolean skipDiscover = true;

    public OPCUAAdapter(String url, boolean secure, boolean skipDiscover) {
        this.url = url;
        this.secure = secure;
        this.skipDiscover = skipDiscover;
    }

    @Override
    public void connect() {
        try {
            Application application = new Application();
            Client client = new Client(application);
            application.addLocale(Locale.ENGLISH);
            application.setApplicationName( new LocalizedText("DatonisGateway", Locale.ENGLISH) );
            application.setProductUri("urn:DatonisGateway" );
            application.setApplicationUri("urn:" + InetAddress.getLocalHost().getHostName() + ":DatonisGateway");

            if (url == null) {
                logger.error("No URL specified for OPC server");
                shutdown();
            }
            if (secure == true) {
                CertificateUtils.setKeySize(1024); // default = 1024
                KeyPair pair = OPCUAAdapter.getCertificate("DatonisGateway");
                application.addApplicationInstanceCertificate( pair );
                session = client.createSessionChannel(url);
            } else {
                if (skipDiscover == false) {
                    EndpointDescription[] endPoints = client.discoverEndpoints(url);
                    for (EndpointDescription e : endPoints) {
                        logger.info("Discovered OPC UA endpoint: " + e.getEndpointUrl() + ", mode: " + e.getSecurityMode().toString());
                    }
                    endPoints = EndpointUtil.selectByMessageSecurityMode(endPoints, MessageSecurityMode.None);
                    if (endPoints.length > 0) {
                        session = client.createSessionChannel(endPoints[0]);
                    } else {
                        throw new Exception("No OPC UA endpoint found with None Security");
                    }
                } else {
                    EndpointDescription ed = new EndpointDescription();
                    ed.setEndpointUrl(url);
                    ed.setSecurityMode(MessageSecurityMode.None);
                    session = client.createSessionChannel(ed);
                }
            }
            session.activate();
            logger.info("Session timeout: " + session.getSession().getSessionTimeout());
            connected = true;
        } catch (Exception e) {
            logger.error("Could not connect with the OPC Server", e);
            shutdown();
        }
    }

    @Override
    public void shutdown() {
        try {
            logger.info("Stopping adapter...");
            if (session != null) {
                session.close();
            }
            logger.info("Stopped OPC UA adapter");
        } catch (ServiceFaultException e) {
            e.printStackTrace();
            logger.error("Could not stop OPC UA adapter", e);
        } catch (ServiceResultException e) {
            logger.error("Could not stop OPC UA adapter", e);
        }
    }

    @Override
    public Object[] readTagValues(String[] tags) throws IllegalArgumentException {
        Object tagValues[] = new Object[tags.length];
        ReadValueId[] arr = new ReadValueId [tags.length];
        int i = 0;
        for (String tag : tags) {
            arr[i++] = new ReadValueId(NodeId.parseNodeId(tag), Attributes.Value, null, null);
        }
        try {
            ReadResponse res = session.Read(null, 1.0, TimestampsToReturn.Source, arr);
            DataValue[] dvs = res.getResults();
            if (dvs != null && dvs.length > 0) {
                for (int j = 0; j < dvs.length; j++) {
                    DataValue dv = dvs[j];
                    if (dv.getStatusCode().equalsStatusCode(StatusCode.GOOD)) {
                        tagValues[j] = dv.getValue().getValue();
                    } else {
                        logger.error("Could not read value for tag: " + tags[j] + ", error: " + dv.getStatusCode());
                        throw new IllegalArgumentException("Could not read value for tag: " + tags[j] + ", error: " + dv.getStatusCode());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Could not read value for tags: " + tags, e);
            connect();
        }
        return tagValues;
    }

    @Override
    public void writeTagValues(String[] tags, Object[] values) throws IllegalArgumentException {
        WriteValue[] arr = new WriteValue[tags.length];
        int i = 0;
        for (String tag : tags) {
            Variant variant = new Variant(values[i]);
            arr[i++] = new WriteValue(NodeId.parseNodeId(tag), Attributes.Value, null, new DataValue(variant));
        }
        try {
            WriteResponse writeResponse = session.Write(null, arr);
            StatusCode[] statusCodes = writeResponse.getResults();
            int j = 0;
            for(StatusCode statusCode : statusCodes) {
                if(!statusCode.equalsStatusCode(StatusCode.GOOD)) {
                    logger.error("Could not write value for tag: " + tags[j] + ", error: " + statusCode);
                }
                j++;
            }
        } catch (Exception e) {
            logger.error("Could not write value for tags: " + tags, e);
            connect();
        }
    }

    private static KeyPair getCertificate(String applicationName) throws ServiceResultException {
        File certFile = new File("/data/user/0/io.datonis.aliothmi/files/" + applicationName + ".der");
        File privKeyFile =  new File("/data/user/0/io.datonis.aliothmi/files/" + applicationName+ ".pem");
        try {
            Cert myServerCertificate = Cert.load( certFile );
            PrivKey myServerPrivateKey = PrivKey.load(privKeyFile, PRIVKEY_PASSWORD);
            return new KeyPair(myServerCertificate, myServerPrivateKey);
        } catch (CertificateException e) {
            throw new ServiceResultException( e );
        } catch (IOException e) {
            try {
                String hostName = InetAddress.getLocalHost().getHostName();
                String applicationUri = "urn:" + hostName + ":" + applicationName;
                KeyPair keys = CertificateUtils.createApplicationInstanceCertificate(applicationName, null, applicationUri, 3650, hostName);
                keys.getCertificate().save(certFile);
                keys.getPrivateKey().save(privKeyFile);
                return keys;
            } catch (Exception e1) {
                throw new ServiceResultException( e1 );
            }
        } catch (Exception e) {
            throw new ServiceResultException( e );
        }
    }

    private void browseTree() {
        Queue<NodeId> queue = new LinkedBlockingQueue<>();
        queue.add(Identifiers.RootFolder);

        while (!queue.isEmpty()) {
            NodeId nodeId = queue.poll();
            try {

                BrowseDescription browse = new BrowseDescription();
                browse.setNodeId(nodeId);
                browse.setBrowseDirection(BrowseDirection.Forward);
                browse.setIncludeSubtypes(true);
                browse.setNodeClassMask(NodeClass.Object, NodeClass.Variable);
                browse.setResultMask(BrowseResultMask.All);
                BrowseResponse res = session.Browse( null, null, null, browse );
                for (BrowseResult result : res.getResults()) {
                    if (result != null && result.getReferences() != null) {
                        for (ReferenceDescription ref : result.getReferences()) {
                            Object value = ref.getNodeId().getValue();
                            if (value instanceof String) {
                                queue.add(new NodeId(ref.getNodeId().getNamespaceIndex(), (String)value));
                            } else if (value instanceof UnsignedInteger) {
                                queue.add(new NodeId(ref.getNodeId().getNamespaceIndex(), (UnsignedInteger)value));
                            }
                            logger.info("Node found: Browsename: " + ref.getBrowseName() + ", NodeId=" + ref.getNodeId());
                        }
                    }
                }
            } catch (ServiceFaultException e) {
                logger.warn("Could not browse tree for: ns=" + nodeId.getNamespaceIndex() + ";value=" + nodeId.getValue(), e);
            } catch (ServiceResultException e) {
                logger.warn("Could not browse tree for: ns=" + nodeId.getNamespaceIndex() + ";value=" + nodeId.getValue(), e);
            }
        }
    }
}
