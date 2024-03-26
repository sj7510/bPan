package onem.baymax.pan.server.module.file.context;

import java.io.Serializable;
import lombok.Data;
import onem.baymax.pan.server.module.file.entity.BPanUserFile;

/**
 * 文件重命名参数上下文对象
 *
 * @author hujiabin wrote in 2024/3/26 18:00
 */
@Data
public class UpdateFilenameContext implements Serializable {

    private static final long serialVersionUID = 6171000069397717175L;

    /**
     * 要更新的文件ID
     */
    private Long fileId;

    /**
     * 新的文件名称
     */
    private String newFilename;

    /**
     * 当前的登录用户ID
     */
    private Long userId;

    /**
     * 要更新的文件记录实体
     */
    private BPanUserFile entity;

}
