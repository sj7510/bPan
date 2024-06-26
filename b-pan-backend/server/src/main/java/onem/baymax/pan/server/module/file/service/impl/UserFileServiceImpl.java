package onem.baymax.pan.server.module.file.service.impl;

import javax.annotation.Nonnull;
import javax.annotation.Resource;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import onem.baymax.pan.core.constant.BPanConstant;
import onem.baymax.pan.core.exception.BPanBusinessException;
import onem.baymax.pan.core.util.FileUtils;
import onem.baymax.pan.core.util.IdUtil;
import onem.baymax.pan.server.common.event.file.DeleteFileEvent;
import onem.baymax.pan.server.module.file.constant.FileConstant;
import onem.baymax.pan.server.module.file.context.CreateFolderContext;
import onem.baymax.pan.server.module.file.context.DeleteFileContext;
import onem.baymax.pan.server.module.file.context.FileSaveContext;
import onem.baymax.pan.server.module.file.context.FileUploadContext;
import onem.baymax.pan.server.module.file.context.QueryFileListContext;
import onem.baymax.pan.server.module.file.context.QueryRealFileListContext;
import onem.baymax.pan.server.module.file.context.SecUploadFileContext;
import onem.baymax.pan.server.module.file.context.UpdateFilenameContext;
import onem.baymax.pan.server.module.file.converter.FileConverter;
import onem.baymax.pan.server.module.file.entity.BPanFile;
import onem.baymax.pan.server.module.file.entity.BPanUserFile;
import onem.baymax.pan.server.module.file.enums.DelFlagEnum;
import onem.baymax.pan.server.module.file.enums.FileTypeEnum;
import onem.baymax.pan.server.module.file.enums.FolderFlagEnum;
import onem.baymax.pan.server.module.file.service.IFileService;
import onem.baymax.pan.server.module.file.service.IUserFileService;
import onem.baymax.pan.server.module.file.mapper.BPanUserFileMapper;
import onem.baymax.pan.server.module.file.vo.BPanUserFileVo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author hujiabin
 */
@Service(value = "userFileService")
public class UserFileServiceImpl extends ServiceImpl<BPanUserFileMapper, BPanUserFile>
        implements IUserFileService, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Resource
    private IFileService fileService;

    @Resource
    private FileConverter fileConverter;

    @Override
    public Long createFolder(CreateFolderContext createFolderContext) {
        return saveUserFile(createFolderContext.getParentId(),
                createFolderContext.getFolderName(),
                FolderFlagEnum.YES,
                null,
                null,
                createFolderContext.getUserId(),
                null);
    }

    @Override
    public BPanUserFile getUserRootFile(Long userId) {
        QueryWrapper<BPanUserFile> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("parent_id", FileConstant.TOP_PARENT_ID);
        queryWrapper.eq("del_flag", DelFlagEnum.NO.getCode());
        queryWrapper.eq("folder_flag", FolderFlagEnum.YES.getCode());
        return getOne(queryWrapper);
    }

    @Override
    public List<BPanUserFileVo> getFileList(QueryFileListContext context) {
        return baseMapper.selectFileList(context);
    }

    @Override
    public void updateFilename(UpdateFilenameContext context) {
        // 1、校验更新文件名称的条件
        checkUpdateFilenameCondition(context);
        // 2、执行更新文件名称的操作
        doUpdateFilename(context);
    }

    @Override
    public void deleteFile(DeleteFileContext context) {
        checkFileDeleteCondition(context);
        doDeleteFile(context);
        afterFileDelete(context);
    }

    /**
     * 文件秒传功能
     * <p>
     * 1、判断用户之前是否上传过该文件
     * 2、如果上传过该文件，只需要生成一个该文件和当前用户在指定文件夹下面的关联关系即可
     *
     * @param context context
     * @return true 代表用户之前上传过相同文件并成功挂在了关联关系 false 用户没有上传过该文件，请手动执行上传逻辑
     */
    @Override public boolean secUpload(SecUploadFileContext context) {
        List<BPanFile> fileList = getFileListByUserIdAndIdentifier(context.getUserId(), context.getIdentifier());
        if (CollectionUtils.isEmpty(fileList)) {
            return false;
        }
        BPanFile record = fileList.get(BPanConstant.ZERO_INT);
        if (Objects.nonNull(record)) {
            // 生成一个该文件和当前用户在指定文件夹下面的关联关系
            saveUserFile(context.getParentId(),
                    context.getFilename(),
                    FolderFlagEnum.NO,
                    FileTypeEnum.getFileTypeCode(FileUtils.getFileSuffix(context.getFilename())),
                    record.getFileId(),
                    context.getUserId(),
                    record.getFileSizeDesc());
            return true;
        }

        return false;
    }

    /**
     * 单文件上传
     * <p>
     * 1、上传文件并保存实体文件的记录
     * 2、保存用户文件的关系记录
     *
     * @param context context
     */
    @Transactional(rollbackFor = Exception.class)
    @Override public void upload(FileUploadContext context) {
        saveFile(context);
        saveUserFile(context.getParentId(),
                context.getFilename(),
                FolderFlagEnum.NO,
                FileTypeEnum.getFileTypeCode(FileUtils.getFileSuffix(context.getFilename())),
                context.getRecord().getFileId(),
                context.getUserId(),
                context.getRecord().getFileSizeDesc());
    }

    /**
     * 上传文件并保存实体文件记录
     * 委托给实体文件的Service去完成该操作
     *
     * @param context context
     */
    private void saveFile(FileUploadContext context) {
        FileSaveContext fileSaveContext = fileConverter.fileUploadContext2FileSaveContext(context);
        fileService.saveFile(fileSaveContext);
        context.setRecord(fileSaveContext.getRecord());
    }

    private void afterFileDelete(DeleteFileContext context) {
        DeleteFileEvent deleteFileEvent = new DeleteFileEvent(this, context.getFileIdList());
        applicationContext.publishEvent(deleteFileEvent);
    }

    private void doDeleteFile(DeleteFileContext context) {
        List<Long> fileIdList = context.getFileIdList();

        UpdateWrapper<BPanUserFile> updateWrapper = new UpdateWrapper<>();
        updateWrapper.in("file_id", fileIdList);
        updateWrapper.set("del_flag", DelFlagEnum.YES.getCode());
        updateWrapper.set("update_time", new Date());

        if (!update(updateWrapper)) {
            throw new BPanBusinessException("文件删除失败");
        }
    }

    /**
     * 删除文件之前的前置校验
     * <p>
     * 1、文件ID合法校验
     * 2、用户拥有删除该文件的权限
     *
     * @param context context
     */
    private void checkFileDeleteCondition(DeleteFileContext context) {
        List<Long> fileIdList = context.getFileIdList();

        List<BPanUserFile> bPanUserFiles = listByIds(fileIdList);
        if (bPanUserFiles.size() != fileIdList.size()) {
            throw new BPanBusinessException("存在不合法的文件记录");
        }

        Set<Long> fieldIdSet = bPanUserFiles.stream().map(BPanUserFile::getFileId).collect(Collectors.toSet());
        int oldSize = fieldIdSet.size();
        fieldIdSet.addAll(fileIdList);
        int newSize = fieldIdSet.size();

        if (oldSize != newSize) {
            throw new BPanBusinessException("存在不合法的文件记录");
        }

        Set<Long> userIdSet = bPanUserFiles.stream().map(BPanUserFile::getUserId).collect(Collectors.toSet());
        if (userIdSet.size() != 1) {
            throw new BPanBusinessException("存在不合法的文件记录");
        }

        Long dbUserId = userIdSet.stream().findFirst().get();
        if (!Objects.equals(dbUserId, context.getUserId())) {
            throw new BPanBusinessException("当前登录用户没有删除该文件的权限");
        }

    }

    /**
     * 执行文件重命名的操作
     *
     * @param context context
     */
    private void doUpdateFilename(UpdateFilenameContext context) {
        BPanUserFile entity = context.getEntity();
        entity.setFilename(context.getNewFilename());
        entity.setUpdateUser(context.getUserId());
        entity.setUpdateTime(new Date());

        if (!updateById(entity)) {
            throw new BPanBusinessException("文件重命名失败");
        }
    }

    /**
     * 更新文件名称的条件校验
     * <p>
     * 1、文件ID是有效的
     * 2、用户有权限更新该文件的文件名称
     * 3、新旧文件名称不能一样
     * 4、不能使用当前文件夹下面的子文件的名称
     *
     * @param context context
     */
    private void checkUpdateFilenameCondition(UpdateFilenameContext context) {
        Long fileId = context.getFileId();
        BPanUserFile entity = getById(fileId);

        if (Objects.isNull(entity)) {
            throw new BPanBusinessException("该文件ID无效");
        }

        if (!Objects.equals(entity.getUserId(), context.getUserId())) {
            throw new BPanBusinessException("当前登录用户没有修改该文件名称的权限");
        }

        if (Objects.equals(entity.getFilename(), context.getNewFilename())) {
            throw new BPanBusinessException("请换一个新的文件名称来修改");
        }

        QueryWrapper<BPanUserFile> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", entity.getParentId());
        queryWrapper.eq("filename", context.getNewFilename());
        int count = count(queryWrapper);

        if (count > 0) {
            throw new BPanBusinessException("该文件名称已被占用");
        }

        context.setEntity(entity);
    }

    private Long saveUserFile(Long parentId,
            String filename,
            FolderFlagEnum folderFlagEnum,
            Integer fileType,
            Long realFileId,
            Long userId,
            String fileSizeDesc) {
        // 保存用户文件的映射记录
        BPanUserFile entity = assemblePanUserFile(parentId, userId, filename, folderFlagEnum, fileType, realFileId, fileSizeDesc);
        if (!save((entity))) {
            throw new BPanBusinessException("保存文件信息失败");
        }
        return entity.getFileId();
    }

    private BPanUserFile assemblePanUserFile(Long parentId, Long userId, String filename, FolderFlagEnum folderFlagEnum,
            Integer fileType, Long realFileId, String fileSizeDesc) {
        // 用户文件映射关系实体转化
        // 构建并填充实体
        BPanUserFile entity = new BPanUserFile();

        entity.setFileId(IdUtil.get());
        entity.setUserId(userId);
        entity.setParentId(parentId);
        entity.setRealFileId(realFileId);
        entity.setFilename(filename);
        entity.setFolderFlag(folderFlagEnum.getCode());
        entity.setFileSizeDesc(fileSizeDesc);
        entity.setFileType(fileType);
        entity.setDelFlag(DelFlagEnum.NO.getCode());
        entity.setCreateUser(userId);
        entity.setCreateTime(new Date());
        entity.setUpdateUser(userId);
        entity.setUpdateTime(new Date());

        // 处理文件命名一致的问题
        handleDuplicateFilename(entity);

        return entity;
    }

    private void handleDuplicateFilename(BPanUserFile entity) {
        /* 处理用户重复名称
         * 如果同一文件夹下面有文件名称重复
         * 按照系统级规则重命名文件
         */
        String filename = entity.getFilename(),
                newFilenameWithoutSuffix,
                newFilenameSuffix;
        int newFilenamePointPosition = filename.lastIndexOf(BPanConstant.POINT_STR);
        if (newFilenamePointPosition == BPanConstant.MINUS_ONE_INT) {
            newFilenameWithoutSuffix = filename;
            newFilenameSuffix = StringUtils.EMPTY;
        } else {
            newFilenameWithoutSuffix = filename.substring(BPanConstant.ZERO_INT, newFilenamePointPosition);
            newFilenameSuffix = filename.replace(newFilenameWithoutSuffix, StringUtils.EMPTY);
        }

        // 查找同名文件数量
        int count = getDuplicateFilename(entity, newFilenameWithoutSuffix);

        if (count == 0) {
            return;
        }

        String newFilename = assembleNewFilename(newFilenameWithoutSuffix, count, newFilenameSuffix);
        entity.setFilename(newFilename);
    }

    private int getDuplicateFilename(BPanUserFile entity, String newFilenameWithoutSuffix) {
        QueryWrapper<BPanUserFile> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", entity.getParentId());
        queryWrapper.eq("folder_flag", entity.getFolderFlag());
        queryWrapper.eq("user_id", entity.getUserId());
        queryWrapper.eq("del_flag", DelFlagEnum.NO.getCode());
        queryWrapper.likeLeft("filename", newFilenameWithoutSuffix);
        return count(queryWrapper);
    }

    private String assembleNewFilename(String newFilenameWithoutSuffix, int count, String newFilenameSuffix) {
        /*
         * 拼装新文件名称
         * 拼装规则参考操作系统重复文件名称的重命名规范
         */
        return newFilenameWithoutSuffix +
                FileConstant.CN_LEFT_PARENTHESES_STR +
                count +
                FileConstant.CN_RIGHT_PARENTHESES_STR +
                newFilenameSuffix;
    }

    /**
     * 查询用户文件列表根据文件的唯一标识
     *
     * @param userId 用户id
     * @param identifier 文件唯一标识
     * @return list
     */
    private List<BPanFile> getFileListByUserIdAndIdentifier(Long userId, String identifier) {
        QueryRealFileListContext context = new QueryRealFileListContext();
        context.setUserId(userId);
        context.setIdentifier(identifier);
        return fileService.getFileList(context);
    }

    @Override public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}




