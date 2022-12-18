package com.cz.demo.hotdeploy;

import java.io.IOException;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

/**
 * @author chenzhang
 * @date 2022/12/15 11:24 上午
 */
public class HotDeployAttach {
    public static void main(String[] args)
        throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
            if (descriptor.displayName().equals("com.cz.demo.hotdeploy.service.ApplicationMain")) {
                VirtualMachine virtualMachine = VirtualMachine.attach(descriptor.id());
                virtualMachine.loadAgent("/Users/dzsb-002298/project/java-agent-learning/agent-hot-deploy/target/agent-hot-deploy-1.0-SNAPSHOT.jar");
                virtualMachine.detach();
            }
        }
    }
}
