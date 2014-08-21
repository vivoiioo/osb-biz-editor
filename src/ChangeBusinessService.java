import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

import weblogic.management.jmx.MBeanServerInvocationHandler;
import weblogic.management.mbeanservers.domainruntime.DomainRuntimeServiceMBean;

import com.bea.wli.config.Ref;
import com.bea.wli.sb.management.configuration.ALSBConfigurationMBean;
import com.bea.wli.sb.management.configuration.ServiceConfigurationMBean;
import com.bea.wli.sb.management.configuration.SessionManagementMBean;
import com.bea.wli.sb.management.query.BusinessServiceQuery;
import com.bea.wli.sb.transports.EndPointConfiguration;
import com.bea.wli.sb.transports.http.HttpEndPointConfiguration;
import com.bea.wli.sb.transports.http.HttpOutboundPropertiesType;
import com.bea.wli.sb.transports.http.HttpUtil;

public class ChangeBusinessService {

	static public void main(String[] args) {

		SessionManagementMBean sm = null;

		if (args.length != 9) {
			System.out
					.println("Falta de argumentos.\n Por gentileza executar com os seguintes parametros:"
							+ "java ChangeBusinessService <nome_sessao> <host> <port> <user> <password> <workmanager> <connection_timeout> <operacao> <timeout_operacao>");
			System.exit(0);
		}
		String host = args[1];
		int port = Integer.parseInt(args[2]);
		String user = args[3];
		String password = args[4];
		String workmanager = args[5];
		int connectionTimeout = Integer.parseInt(args[6]);
		int operacao = Integer.parseInt(args[7]);
		long operacaoTimeout = Long.parseLong(args[8]);

		JMXConnector conn = initConnection(host, port, user, password);

		try {
            long beginGlobalTime = System.currentTimeMillis();
            
			MBeanServerConnection mbconn = conn.getMBeanServerConnection();

			DomainRuntimeServiceMBean domainService = (DomainRuntimeServiceMBean) MBeanServerInvocationHandler
					.newProxyInstance(mbconn, new ObjectName(
							DomainRuntimeServiceMBean.OBJECT_NAME));

			sm = (SessionManagementMBean) domainService.findService(
					SessionManagementMBean.NAME, SessionManagementMBean.TYPE,
					null);

			String sess1 = args[0];
			System.out.println("Criando Sessao");
			sm.createSession(sess1);

			ALSBConfigurationMBean alsbSession = (ALSBConfigurationMBean) domainService
					.findService(ALSBConfigurationMBean.NAME + "." + sess1,
							ALSBConfigurationMBean.TYPE, null);

			Set<Ref> refs2 = alsbSession.getRefs(Ref.DOMAIN);
			System.out.println("Listando servicos");

			int contador = 0;
			for (Ref ref : refs2) {
				if(System.currentTimeMillis() - beginGlobalTime >= operacaoTimeout){
					System.out.println("Estourou o tempo de execução. \n iniciando processo de ativacao.");
					break;
				}
				if (ref.getTypeId().equalsIgnoreCase("BusinessService")) {

					String localPath = ref.getFullName().replaceAll("(.*)/.*",
							"$1");

					BusinessServiceQuery bsQuery = new BusinessServiceQuery();
					bsQuery.setLocalName(ref.getLocalName());
					bsQuery.setPath(localPath);
					Set<Ref> refs = alsbSession.getRefs(bsQuery);
					com.bea.wli.config.Ref bsRef = (com.bea.wli.config.Ref) refs
							.iterator().next();

					final Set<ObjectName> mbeans = new HashSet<ObjectName>();
					ObjectName objName = new ObjectName(
							"com.bea:Name=ServiceConfiguration"
									+ "."
									+ sess1
									+ ",Type=com.bea.wli.sb.management.configuration.ServiceConfigurationMBean");
					mbeans.addAll(mbconn.queryNames(objName, null));
					ServiceConfigurationMBean serviceConfigMBean = (ServiceConfigurationMBean) MBeanServerInvocationHandler
							.newProxyInstance(conn, mbeans.iterator().next(),
									ServiceConfigurationMBean.class, false);

					com.bea.wli.sb.services.ServiceDefinition serviceDef = serviceConfigMBean
							.getServiceDefinition(bsRef);

					EndPointConfiguration endpointConfigration = serviceDef
							.getEndpointConfig();

					if (endpointConfigration.getProviderId().equalsIgnoreCase(
							"http")) {
                        
						System.out.println("Inicio do processo do BusinessService: " + bsQuery.getLocalName());
						long start = System.currentTimeMillis();
                        
						HttpEndPointConfiguration httpEndPointConf = HttpUtil
								.getHttpConfig(endpointConfigration);

						HttpOutboundPropertiesType outboundProps = null;
						
						System.out.println("WorkManager do servico "+ bsQuery.getLocalName()+ ": " + httpEndPointConf.getDispatchPolicy());
						
						if( httpEndPointConf.getDispatchPolicy() == null || !httpEndPointConf.getDispatchPolicy().equalsIgnoreCase(workmanager)){
											
							switch (operacao) {
							case 1:
								// setando o workmanager
								httpEndPointConf.setDispatchPolicy(workmanager);
								
								outboundProps = httpEndPointConf
										.getOutboundProperties();
								break;
								
							case 2:
								outboundProps = httpEndPointConf
								.getOutboundProperties();
								
								// setando connection timeout
								outboundProps
								.setConnectionTimeout(connectionTimeout);
								break;
								
							case 3:
								// setando o workmanager
								httpEndPointConf.setDispatchPolicy(workmanager);
								
								outboundProps = httpEndPointConf
										.getOutboundProperties();
								
								// setando connection timeout
								outboundProps
								.setConnectionTimeout(connectionTimeout);
								
								break;
								
							default:
								outboundProps = httpEndPointConf
								.getOutboundProperties();
								break;
							}
							
							httpEndPointConf.setOutboundProperties(outboundProps);

							endpointConfigration
									.setProviderSpecific(httpEndPointConf);

							serviceConfigMBean.updateService(bsRef, serviceDef);
							
							long finish = System.currentTimeMillis();
							
							long total = finish - start;
							
							double totalSegundos = total/1000;
							
							System.out.println("Tempo total com alteração do BusinessService " + bsQuery.getLocalName() + " : " + totalSegundos + " segundos." );

							contador++;
						
							
						}else{
							long finish = System.currentTimeMillis();
							long total = finish - start;
							double totalSegundos = total/1000;
							System.out.println("Tempo total sem alteração do BusinessService " + bsQuery.getLocalName() + " : " + totalSegundos + " segundos." );
							System.out.println("BusinessService " +bsQuery.getLocalName() + " nao foi alterado.");
						}

						

					}

				}

			}
			if(contador == 0){
				sm.discardSession(sess1);
				System.out.println("Sessao descartada, pois nao foram alterados BusinessServices");
			    	
			}else {
				System.out.println(contador
						+ " BusinessService alterado.");
				System.out.println("ativando a sessao");
				sm.activateSession(sess1, "alteração de workmanager e connectionTimeOut");
				System.out.println("sessao ativa");
			}
			conn.close();
			
		} catch (Exception e) {
			System.out
					.println("Aconteceu algum erro ao executar o script. \n sua sessão sera descartada");
			e.printStackTrace();
			try {
				sm.discardSession(args[0]);

			} catch (Exception e1) {
				System.out.println("Ocorreu um erro ao descartar a sessão.");
				e1.printStackTrace();

			}

		}

	}

	public static JMXConnector initConnection(String hostname, int port,
			String username, String password) {
		JMXServiceURL serviceURL = null;
		Hashtable<String, String> h = new Hashtable<String, String>();
		JMXConnector jmx = null;

		try {

			System.out.println("Iniciando conexão");

			serviceURL = new JMXServiceURL("t3", hostname, port, "/jndi/"
					+ DomainRuntimeServiceMBean.MBEANSERVER_JNDI_NAME);

			h.put(Context.SECURITY_PRINCIPAL, username);
			h.put(Context.SECURITY_CREDENTIALS, password);
			h.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES,
					"weblogic.management.remote");
			jmx = JMXConnectorFactory.connect(serviceURL, h);

		} catch (Exception e) {
			System.out.println("Erro ao iniciar a conexão");
		}
		return jmx;

	}
}