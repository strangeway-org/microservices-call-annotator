package org.strangeway.msa.frameworks

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.uast.UCallExpression
import org.strangeway.msa.db.InteractionType

class GrpcCallDetector : CallDetector {
  private val interaction: Interaction = FrameworkInteraction(InteractionType.REQUEST, "gRPC")
  private val streamInteraction: Interaction = FrameworkInteraction(InteractionType.MESSAGE_RECEIVE, "gRPC Stream")

  override fun isAvailable(project: Project): Boolean {
    return hasLibraryClass(project, "io.grpc.stub.annotations.RpcMethod")
  }

  override fun getCallInteraction(project: Project, uCall: UCallExpression): Interaction? {
    val method = uCall.resolve() ?: return null
    val clazz = method.containingClass ?: return null

    if (isGrpcStub(clazz)) {
      val returnClass = (method.returnType as? PsiClassType)?.rawType()?.resolve()
      if (returnClass != null) {
        if (returnClass.qualifiedName == "io.grpc.stub.StreamObserver") {
          return streamInteraction
        }

        if (returnClass.qualifiedName == "com.google.common.util.concurrent.ListenableFuture"
          || InheritanceUtil.isInheritor(returnClass, "com.google.protobuf.GeneratedMessageV3")
        ) {
          return interaction
        }
      }

      if (method.parameterList.parametersCount == 2) {
        val secondParam = method.parameterList.getParameter(1)
        if (secondParam != null) {
          val paramType = (secondParam.type as? PsiClassType)?.rawType()?.resolve()
          if (paramType != null && paramType.qualifiedName == "io.grpc.stub.StreamObserver") {
            return interaction
          }
        }
      }
    }

    return null
  }

  private fun isGrpcStub(clazz: PsiClass): Boolean {
    val exactSuper = clazz.superClass ?: return false
    val qualifiedName = exactSuper.qualifiedName
    return qualifiedName == "io.grpc.stub.AbstractBlockingStub"
        || qualifiedName == "io.grpc.stub.AbstractFutureStub"
        || qualifiedName == "io.grpc.stub.AbstractAsyncStub"
        || qualifiedName == "io.grpc.stub.AbstractStub"
  }
}