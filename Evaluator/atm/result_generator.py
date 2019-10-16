import csv
import os
import json
import numpy
import copy

def levenshtein(seq1, seq2):
    # delete 'NONE' events in order to calculate levenshtein distance correctly
    events1 = copy.deepcopy(seq1)
    events2 = copy.deepcopy(seq2)
    for event in events1:
        if event == 'NONE':
            events1.remove(event)
    for event in events2:
        if event == 'NONE':
            events2.remove(event)

    size_x = len(events1) + 1
    size_y = len(events2) + 1
    matrix = numpy.zeros ((size_x, size_y))
    for x in range(size_x):
        matrix [x, 0] = x
    for y in range(size_y):
        matrix [0, y] = y

    for x in range(1, size_x):
        for y in range(1, size_y):
            if events1[x-1] == events2[y-1]:
                matrix [x,y] = min(
                    matrix[x-1, y] + 1,
                    matrix[x-1, y-1],
                    matrix[x, y-1] + 1
                )
            else:
                matrix [x,y] = min(
                    matrix[x-1,y] + 1,
                    matrix[x-1,y-1] + 1,
                    matrix[x,y-1] + 1
                )
    # print (matrix)
    return (matrix[size_x - 1, size_y - 1])



def evaluate_atm_mapping(src_app, tgt_app):
    with open(os.path.join('../../test_csv/', src_app + '.csv'), 'r') as test_input:
        with open('final_results_atm.csv', 'a') as result_output:
            writer = csv.writer(result_output, lineterminator='\n')
            reader = csv.reader(test_input)
            for row in reader:
                src_test = []
                trans_test = []
                correct = []
                incorrect = []
                missed = []
                nonExist = []
                # only evaluate sign in and sign up
                if 'testSignIn()' in row[0] or 'testSignUp()' in row[0]:
                    event_array = json.loads(row[1])
                    for event in event_array:
                        if 'id@' in event['id_or_xpath']:
                            src_id = event['id_or_xpath'].split('id@')[1]
                            src_test.append(src_id)
                            src_can = find_canonical(src_id, src_app)
                            trans_id = get_trans_id(src_id, src_app, tgt_app)
                            trans_can = find_canonical(trans_id, tgt_app)
                            trans_test.append(trans_id)
                            if trans_can != 'NONE':
                                if src_can == trans_can:
                                    correct.append(src_id)
                                else:
                                    incorrect.append(src_id)
                            else:
                                if check_canonical(src_can, tgt_app):
                                    # missed case
                                    missed.append(src_id)
                                else:
                                    #nonExist case
                                    nonExist.append(src_id)
                        else:
                            src_test.append(event['id_or_xpath'])
                            print ('no id found for event ', event)

                    # calcuate TP, FP, FN by comparing transferred test with ground-truth test
                    method_name = str(row[0]).split(': ')[1]
                    gt_test = get_gt_test(tgt_app, method_name)
                    print ('method = ', row[0])
                    print ('gt test = ', gt_test)
                    print ('trans test = ', trans_test)
                    TP = set(trans_test) & set(gt_test)
                    FP = set(trans_test) - set(gt_test)
                    FN = set(gt_test) - set(trans_test)
                    # output the current test case's results to the file
                    current_result = []
                    current_result.append(row[0])
                    current_result.append(src_test)
                    current_result.append(trans_test)
                    current_result.append(gt_test)
                    current_result.append(src_app)
                    current_result.append(tgt_app)
                    current_result.append('atm')
                    current_result.append(correct)
                    current_result.append(incorrect)
                    current_result.append(missed)
                    current_result.append(nonExist)
                    current_result.append(TP)
                    current_result.append(FP)
                    current_result.append(FN)
                    current_result.append(len(correct))
                    current_result.append(len(incorrect))
                    current_result.append(len(missed))
                    current_result.append(len(nonExist))
                    if (len(correct) + len(incorrect)) == 0:
                        current_result.append('NA')
                    else:
                        current_result.append(len(correct)/(len(correct) + len(incorrect)))
                    if (len(correct) + len(missed)) == 0:
                        current_result.append('NA')
                    else:
                        current_result.append(len(correct) / (len(correct) + len(missed)))
                    current_result.append(levenshtein(trans_test, gt_test))
                    writer.writerow(current_result)

# return ground truth test case's event array
def get_gt_test(tgt_app, method_name):
    gt_test = []
    with open(os.path.join('../../test_csv/', tgt_app + '.csv'), 'r') as test_input:
        reader = csv.reader(test_input)
        for row in reader:
            if method_name in row[0]:
                json_array = json.loads(row[1])
                for event in json_array:
                    id_or_xpath = event['id_or_xpath']
                    if 'id@' in id_or_xpath:
                        gt_test.append(id_or_xpath.split('id@')[1])
                    else:
                        gt_test.append(id_or_xpath)
                return gt_test

def check_canonical(canonical, app):
    with open('../ground_truth_mapping/GUI Mapping Ground Truth - ' + app + '.csv') as canonical_input:
        reader = csv.reader(canonical_input)
        for row in reader:
            if row[3] == canonical:
                return True
    return False

def get_trans_id(src_id, src_app, tgt_app):
    filename = src_app + '_' + tgt_app + '_Mappings.csv'
    with open(filename, 'r') as mapping_input:
        reader = csv.reader(mapping_input)
        for row in reader:
            if row[0] == src_id:
                return row[1]
    return 'NONE'

def find_canonical(id, app):
    with open('../ground_truth_mapping/GUI Mapping Ground Truth - ' + app + '.csv') as canonical_input:
        reader = csv.reader(canonical_input)
        for row in reader:
            if row[0] == id:
                return row[3]
    return 'NONE'

if __name__ == "__main__":

    src_app = 'Wish'
    tgt_app = 'Etsy'
    evaluate_atm_mapping(src_app, tgt_app)
    # result is in 'final_results_atm.csv' with the header 'method,src_events,transferred,gt_events,source,target,gui_mapper,correct,incorrect,missed,nonExist,TP,FP,FN,num_correct,num_incorrect,num_missed,num_nonExist,accuracy_precision,accuracy_recall,distance'
    # a = ['aa', 'bbb', 'ccc']
    # b = ['bb', 'aa', 'ccc']
    # print (levenshtein(b, a))