package com.notaskflow.service;

import com.notaskflow.domain.dto.request.SpaceJoinApplyRequest;
import com.notaskflow.domain.dto.request.SpaceJoinApproveRequest;
import com.notaskflow.domain.dto.request.SpaceJoinRejectRequest;
import com.notaskflow.domain.vo.SpaceJoinRequestVO;
import java.util.List;

/**
 * 团队加入申请服务接口。
 *
 * @author LIN
 */
public interface SpaceJoinRequestService {

    /**
     * 创建团队加入申请。
     *
     * @param request 创建请求
     * @return 申请信息
     */
    SpaceJoinRequestVO apply(SpaceJoinApplyRequest request);

    /**
     * 查询当前用户提交的加入申请。
     *
     * @return 申请列表
     */
    List<SpaceJoinRequestVO> listMine();

    /**
     * 查询当前用户待处理的加入申请。
     *
     * @return 申请列表
     */
    List<SpaceJoinRequestVO> listPendingForMe();

    /**
     * 审批通过加入申请。
     *
     * @param requestId 申请标识
     * @param request 审批请求
     * @return 申请信息
     */
    SpaceJoinRequestVO approve(Long requestId, SpaceJoinApproveRequest request);

    /**
     * 拒绝加入申请。
     *
     * @param requestId 申请标识
     * @param request 拒绝请求
     * @return 申请信息
     */
    SpaceJoinRequestVO reject(Long requestId, SpaceJoinRejectRequest request);
}
