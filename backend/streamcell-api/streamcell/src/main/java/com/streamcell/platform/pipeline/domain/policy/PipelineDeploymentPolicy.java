package com.streamcell.platform.pipeline.domain.policy;

import com.streamcell.platform.pipeline.enums.PipelineStatus;
import com.streamcell.platform.topic.vo.Topic;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PipelineDeploymentPolicy {

//    다음 Kafka Topic을 사용하여 사용자의 요청을 PipelinePlan으로 변환하세요.
//
//    Topic 이름: orders
//    Topic 설명: 주문 발생 이벤트
//
//    사용 가능한 필드:
//    - order_id: STRING
//    - product_id: STRING
//    - payment_amount: DOUBLE
//    - quantity: INT
//    - event_time: TIMESTAMP(3)
//
//    사용자 요청:
//            5분마다 상품별 주문 건수와 평균 결제금액을 계산하고,
//    결제금액이 10000원 이상인 주문만 포함해줘.
    private final String MESSAGE_TEMPLATE = """
            다음 Kafka Topic을 사용하여 사용자의 요청을 PipelinePlan으로 변환하세요.
            
            Topic 이름: %s
            Topic 설명: %s
            
            사용 가능한 필드:
            %s
            
            사용자 요청:
            %s
            """;

    private final List<PipelineStatus> availableDeployPipelineStatus =
            List.of(
                    PipelineStatus.DRAFT,
                    PipelineStatus.CREATED
            );

    public boolean isDeployPipeline(PipelineStatus pipelineStatus) {
        return availableDeployPipelineStatus.contains(pipelineStatus);
    }

    public String getSendMessageByTemplate(Topic topic, String message) {
        return MESSAGE_TEMPLATE.formatted(
                topic.getTopicName(),
                topic.getDescription(),
                topic.getSchemaJson(),
                message
        );
    }
}
